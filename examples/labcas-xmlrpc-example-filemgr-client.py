#!/usr/bin/python
# Example Python script for querying the OODT File Manager through its XML/RPC interface.

import xmlrpclib

def printProductType(productTypeDict):
	print 'PRODUCT TYPE: %s' % productTypeDict['name']
	for key, value in productTypeDict.items():
		print '\t%s = %s' % (key, value)

if __name__ == '__main__':

        # connect to File Manager server
        # use verbose=True to print out every request/response 
        verbose=False
        server = xmlrpclib.ServerProxy('http://localhost:9000/', verbose=verbose)

        # test server is alive
        print "Server is alive: %s" % server.filemgr.isAlive()

        # query all product types
        productTypes =  server.filemgr.getProductTypes()
        for productTypeDict in productTypes:
                printProductType(productTypeDict)

        # retrieve a specific product type
        # returned result is a dictionary suitable as argument for next call
        productTypeName = "miRNAStudyPine"
        productType = server.filemgr.getProductTypeByName(productTypeName)

        # query all products of a given type
        products = server.filemgr.getProductsByProductType( productType )
        print 'Printing products'
        for product in products:
            print product
