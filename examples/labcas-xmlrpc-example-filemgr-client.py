#!/usr/bin/python
# Example Python script for interacting with an OODT FileManager through its XML/RPC interface
# Supported server methods are defined in file org.apache.oodt.cas.filemgr.system.XmlRpcFileManager:
# o public Vector<Hashtable<String, Object>> getProductTypes()
# o public String addProductType(Hashtable<String, Object> productTypeHash()

import xmlrpclib
import time

def listProductTypes(server):
	
	# list all supported product types
	print 'Listing product types in File Manager'
	productTypes =  server.filemgr.getProductTypes()
	for ptMap in productTypes:
		print 'PRODUCT TYPE:'
		for key, value in ptMap.items():
			print '%s = %s' % (key, value)


if __name__ == '__main__':
	
	# connect to File Manager server
	# use verbose=True to print out every request/response 
	verbose=False
	server = xmlrpclib.ServerProxy('http://localhost:9000/', verbose=verbose)
	
	listProductTypes(server)
	
	# add new product type		
	# will override if existing already
	ptHash = { 'id':"XYZ",
			  'name': 'XYZ_Product_Type',
			  'description': 'XYZ product type',
              'repositoryPath' : 'file:///usr/local/labcas_archive/labcas-upload',
              'versionerClass':"gov.nasa.jpl.edrn.labcas.versioning.LabcasProductVersioner",
              "typeExtractors": [{'className': 'org.apache.oodt.cas.filemgr.metadata.extractors.CoreMetExtractor', 
								  'config': {'elementNs': 'CAS', 'elements': 'ProductReceivedTime,ProductName,ProductId,ProductType', 'nsAware': 'true'}}, 
								 {'className': 'org.apache.oodt.cas.filemgr.metadata.extractors.examples.MimeTypeExtractor', 
								  'config': {}}],
              "typeMetadata": {'Version': ['[Version]'], 'Dataset': ['[Dataset]']} }
	
	ptName = server.filemgr.addProductType(ptHash)
	print 'Added product type: %s' % ptName
	
	listProductTypes(server) 
	