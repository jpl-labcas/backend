#!/usr/bin/python
# Example script for querying the OODT File Manager Solr engine
# for datasets that have been published through the "labcas-upload" workflow

import solr

def printResult(result):
    '''Utility function to print out a few fields of a result.'''
    
    print "\nFile id=%s" % result['id'][0]
    print "File name=%s" % result['Filename'][0]
    print "File size=%s" % result['FileSize'][0]
    print "File location=%s" % result['CAS.ReferenceDatastore'][0]
    print "File version=%s" % result['Version'][0]

if __name__ == '__main__':
    
    solr_url = "http://localhost:8080/solr" 
    solr_server = solr.SolrConnection(solr_url)
    
    # query for all datasets with this name, all versions
    response = solr_server.query('*:*', fq=['Dataset:mydata'], start=0)
    print "\nNumber of files found: %s" % response.numFound
    for result in response.results:
        printResult(result)
        
    # query for all possible versions of this dataset
    response = solr_server.query('*:*', fq=['Dataset:mydata'], start=0, rows=0, facet='true', facet_field='Version')
    versions = response.facet_counts['facet_fields']['Version']
    for key, value in versions.items():
        print "\nVersion number %s has %s files" % (key, value)
        
    # query for all files for a specific version
    response = solr_server.query('*:*', fq=['Dataset:mydata','Version:23'], start=0)
    print "\nNumber of files found for this version: %s" % response.numFound
    for result in response.results:
        printResult(result)
