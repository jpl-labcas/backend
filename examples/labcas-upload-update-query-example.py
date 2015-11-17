#!/usr/bin/python
# Complete example for uploading, updating and querying a Labcas dataset.

# Workflow Manager XML/RPC supported methods are defined in org.apache.oodt.cas.workflow.system.XmlRpcWorkflowManager.java:
# o public boolean handleEvent(String eventName, Hashtable metadata)
# o public String executeDynamicWorkflow(Vector<String> taskIds, Hashtable metadata)
# o public Hashtable getWorkflowInstanceById(String wInstId)

# File Manager XML/RPC supported method are defined in org.apache.oodt.cas.filemgr.system.XmlRpcFileManager.java:
# o public Hashtable<String, Object> getProductByName(String productName)
# o public Vector<Hashtable<String, Object>> getProductTypes()

# Solr queries are based on optional module solrpy

import xmlrpclib
import time
import solr

# global objects
verbose = False
workflowManagerServerProxy = xmlrpclib.ServerProxy('http://localhost:9001/', verbose=verbose)
fileManaferServerProxy = xmlrpclib.ServerProxy('http://localhost:9000/', verbose=verbose)
solrServerProxy = solr.SolrConnection("http://localhost:8983/solr/oodt-fm")

	
def uploadDataset(dataset):
	
	# NOTE: currently, if you start a named workflow, the XMLRPC interface only returns True/False, not a workflow instance identifier...
	#tf = serverProxy.workflowmgr.handleEvent('labcas-upload', { 'Dataset':'mydata' } )

	# ... consequently, you must submit an equivalent dynamic workflow, which does return the workflow instance id
	wInstId = workflowManagerServerProxy.workflowmgr.executeDynamicWorkflow( ['urn:edrn:LabcasUploadInitTask','urn:edrn:LabcasUploadExecuteTask'], { 'Dataset':'mydata' } )

	# monitor workflow instance
	waitForCompletion(wInstId)

def updateDataset(dataset):
	
	# submit "labcas-update" workflow
	wInstId = workflowManagerServerProxy.workflowmgr.executeDynamicWorkflow( ['urn:edrn:LabcasUpdateTask'], { 'Dataset':'mydata' } )

	# monitor workflow instance
	waitForCompletion(wInstId)

def waitForCompletion(wInstId):
	''' Monitors a workflow instance until it completes.'''
	
	# wait for the server to instantiate this workflow before querying it
	time.sleep(1) 

	# now use the workflow instance id to check for status, wait until completed
	running_status  = ['CREATED', 'QUEUED', 'STARTED', 'PAUSED']
	pge_task_status = ['STAGING INPUT', 'BUILDING CONFIG FILE', 'PGE EXEC', 'CRAWLING']
	finished_status = ['FINISHED', 'ERROR']
	while (True):
		response = workflowManagerServerProxy.workflowmgr.getWorkflowInstanceById(wInstId)
	        status = response['status']
	        if status in running_status or status in pge_task_status:
	        	print 'Workflow istance=%s running with status=%s' % (wInstId, status)
	        	time.sleep(1)
		elif status in finished_status:
			print 'Workflow istance=%s ended with status=%s' % (wInstId, status)
			break
		else:
			print 'UNRECOGNIZED WORKFLOW STATUS: %s' % status
			break
	print response
	
def getProductType(dataset):
	
	# retrieve a specific product type by name
	productTypeDict =  fileManaferServerProxy.filemgr.getProductTypeByName(dataset)
	
	printProductType(productTypeDict)
	
def listProductTypes():
	
	# list all supported product types
	productTypes =  fileManaferServerProxy.filemgr.getProductTypes()
	for productTypeDict in productTypes:
		printProductType(productTypeDict)

def printProductType(productTypeDict):
	print 'PRODUCT TYPE: %s' % productTypeDict['name']
	for key, value in productTypeDict.items():
		print '\t%s = %s' % (key, value)

def listProducts(dataset):
	
	# query for all datasets with this name, all versions
	response = solrServerProxy.query('*:*', fq=['Dataset:%s' % dataset], start=0)
	print "\nNumber of files found: %s" % response.numFound
	for result in response.results:
		printProduct(result)
		
	# query for all possible versions of this dataset
	response = solrServerProxy.query('*:*', fq=['Dataset:%s' % dataset], start=0, rows=0, facet='true', facet_field='Version')
	versions = response.facet_counts['facet_fields']['Version']
	last_version = 0
	for key, value in versions.items():
		print "\nVersion number %s has %s files" % (key, value)
		if int(key) > last_version:
			last_version = int(key)
		
	# query for all files for a specific version
	response = solrServerProxy.query('*:*', fq=['Dataset:%s' % dataset,'Version:%s' % last_version ], start=0)
	print "\nLatest version: %s number of files: %s" % (last_version, response.numFound)
	for result in response.results:
		printProduct(result)
	
def printProduct(result):
    '''Utility function to print out a few fields of a result.'''
    
    print "\nFile id=%s" % result['id']             # single-valued field
    print "File name=%s" % result['Filename'][0]    # multi-valued field
    print "File size=%s" % result['FileSize'][0]    # multi-valued field
    print "File location=%s" % result['CAS.ReferenceDatastore'][0]  # multi-valued field
    print "File version=%s" % result['Version'][0]  # multi-valued field

	
if __name__ == '__main__':
		
	dataset = 'mydata'
	
	# upload dataset staged in directory 'mydata'
	uploadDataset(dataset)
	
	# upload the dataset again:
	# o a new version will be generated
	# o the product type metadata will be completey overridden
	#uploadDataset(dataset)
	
	# update dataset metadata - no new version is generated
	updateDataset(dataset)
	
	# list all product types in File manager
	#listProductTypes()
	
	# query the product types from the XML/RPC File Manager interface
	getProductType(dataset)
	
	# list all products for given dataset == product type
	listProducts(dataset)