import xmlrpclib
import time
import solr

class LabcasClient(object):
    '''
    Python client used to interact with a remote Labcas back-end.
    Mostly for demo and example purposes...
    Available methods are defined in Java class org.apache.oodt.cas.workflow.system.XmlRpcWorkflowManager.
    '''
    
    def __init__(self, 
                 workflowManagerUrl='http://localhost:9001/',
                 fileManagerUrl='http://localhost:9000/',
                 solrUrl='http://localhost:8983/solr/oodt-fm',
                 verbose=False):
        
        self.workflowManagerServerProxy = xmlrpclib.ServerProxy(workflowManagerUrl, verbose=verbose)
        self.fileManaferServerProxy = xmlrpclib.ServerProxy(fileManagerUrl, verbose=verbose)
        self.solrServerProxy = solr.SolrConnection(solrUrl)
        
    def getWorkflowsByEvent(self, eventName):
        '''Retrieve a specific workflow by the triggering event.'''
        
        workflows =  self.workflowManagerServerProxy.workflowmgr.getWorkflowsByEvent(eventName)
        for workflow in workflows:
            self.printWorkflow(workflow)
        
    def getWorkflowById(self, workflowId):
        '''Retrieve a specific workflow by its unique identifier.'''
        
        workflow =  self.workflowManagerServerProxy.workflowmgr.getWorkflowById(workflowId)
        self.printWorkflow(workflow)
        
    def executeWorkflow(self, tasks, metadata):
        '''Submits a dynamic workflow composed of the specified tasks, using the specified metadata.'''
        
        return self.workflowManagerServerProxy.workflowmgr.executeDynamicWorkflow(tasks, metadata)
        
    def waitForCompletion(self, wInstId):
        ''' Monitors a workflow instance until it completes.'''
    
        # wait for the server to instantiate this workflow before querying it
        time.sleep(2) 
    
        # now use the workflow instance id to check for status, wait until completed
        running_status  = ['CREATED', 'QUEUED', 'STARTED', 'PAUSED']
        pge_task_status = ['STAGING INPUT', 'BUILDING CONFIG FILE', 'PGE EXEC', 'CRAWLING']
        finished_status = ['FINISHED', 'ERROR', 'METMISS']
        while (True):
            response = self.workflowManagerServerProxy.workflowmgr.getWorkflowInstanceById(wInstId)
            status = response['status']
            if status in running_status or status in pge_task_status:
                print 'Workflow instance=%s running with status=%s' % (wInstId, status)
                time.sleep(1)
            elif status in finished_status:
                print 'Workflow instance=%s ended with status=%s' % (wInstId, status)
                break
            else:
                print 'UNRECOGNIZED WORKFLOW STATUS: %s' % status
                break
        print response
        
    def uploadDataset(self, datasetId, metadata, newVersion=False):
        
        # add 'DatasetId' key, value to other metadata
        metadata['DatasetId'] = datasetId
        
        # optionally request a new version
        if newVersion:
            metadata['newVersion'] = 'true'
    
        # NOTE: currently, if you start a named workflow, the XMLRPC interface only returns True/False, not a workflow instance identifier...
        #tf = serverProxy.workflowmgr.handleEvent('labcas-upload', { 'DatasetId':'mydata' } )
    
        # ... consequently, you must submit an equivalent dynamic workflow, which does return the workflow instance id
        wInstId = self.workflowManagerServerProxy.workflowmgr.executeDynamicWorkflow( ['urn:edrn:LabcasUploadInitTask','urn:edrn:LabcasUploadExecuteTask'], 
                                                                                      metadata )
    
        # monitor workflow instance
        self.waitForCompletion(wInstId)
        
    def getProductTypeByName(self, datasetName):
    
        # retrieve a specific product type by name
        productTypeDict =  self.fileManaferServerProxy.filemgr.getProductTypeByName(datasetName)
        
        self.printProductType(productTypeDict)
    
    def getProductTypeById(self, datasetId):
        
        # retrieve a specific product type by name
        productTypeDict =  self.fileManaferServerProxy.filemgr.getProductTypeById(datasetId)
        
        self.printProductType(productTypeDict)
        
    def listProductTypes(self):
        
        # list all supported product types
        productTypes =  self.fileManaferServerProxy.filemgr.getProductTypes()
        for productTypeDict in productTypes:
            self.printProductType(productTypeDict)
    
    def printProductType(self, productTypeDict):
        print 'PRODUCT TYPE: %s' % productTypeDict['name']
        for key, value in productTypeDict.items():
            print '\t%s = %s' % (key, value)
    
    def listProducts(self, datasetId):
        
        # query for all products of this type (i.e. all files of this dataset), all versions
        response = self.solrServerProxy.query('*:*', fq=['DatasetId:%s' % datasetId], start=0)
        print "\nNumber of files found: %s (all versions)" % response.numFound
        for result in response.results:
            self.printProduct(result)
            
        # query for all possible versions of this dataset
        response = self.solrServerProxy.query('*:*', fq=['DatasetId:%s' % datasetId], start=0, rows=0, facet='true', facet_field='Version')
        versions = response.facet_counts['facet_fields']['Version']
        last_version = 0
        for key, value in versions.items():
            print "\nVersion number %s has %s files" % (key, value)
            if int(key) > last_version:
                last_version = int(key)
            
        # query for all files for a specific version
        response = self.solrServerProxy.query('*:*', fq=['DatasetId:%s' % datasetId,'Version:%s' % last_version ], start=0)
        print "\nLatest version: %s number of files: %s, listing them all:" % (last_version, response.numFound)
        for result in response.results:
            self.printProduct(result)
        
    def printProduct(self, result):
        '''Utility function to print out the product metadata'''
        
        print '\nProduct ID=%s' % result['id']
        for key, values in result.items():
            print '\tProduct metadata key=%s values=%s' % (key, values)

    def printWorkflow(self, workflowDict):
        '''Utiliyu function to print out a workflow.'''
        
        print workflowDict
        print "Workflow id=%s name=%s" % (workflowDict['id'], workflowDict['name'])
        for task in workflowDict['tasks']:
            print "Task: %s" % task