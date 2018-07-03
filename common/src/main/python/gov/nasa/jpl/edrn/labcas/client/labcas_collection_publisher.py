import sys
import os

class LabcasCollectionPublisher(object):
    '''
    Publishes all datasets within a collection
    '''
    
    def __init__(self, solr_url='http://localhost:8983/solr'):
        
        self._solr_url = solr_url
        
    def crawl(self, directory_path):
        
        # read collection metadata from configuration file
        
        # loop over all sub-directories to publish datasets
        
        pass