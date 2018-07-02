import ConfigParser
import logging
import os
import sys
from xml.sax.saxutils import escape
import urllib2
from solr_client import SolrClient
from workflow_client import WorkflowManagerClient

from labcas_client import LabcasClient


logging.basicConfig(level=logging.DEBUG)


class LabcasDatasetPublisher(object):
    '''
    Publishes a hierarchy of datasets rooted at some directory.
    '''
    
    def __init__(self, collection_name, solr_url='http://localhost:8983/solr'):
        
        
        self._collection_name = collection_name
        # NOTE: "CollectionId" must match directory location 
        # under $LABCAS_ARCHIVE or $LABCAS_STAGING
        self._collection_id = collection_name.replace(" ", "_")
        
        self._solr_client = SolrClient(solr_url)
        self._wmgr_client = WorkflowManagerClient()
        
            
    def crawl(self, directory_path, dataset_parent_id=None):
        '''
        Recursively parses a directory path and publishes all datasets.
        '''
        
        # remove last '/' otherwise the path is not split correctly
        if directory_path.endswith('/'):
            directory_path = directory_path[:-1]
        logging.info("Crawling directory: %s" % directory_path)
        
        # collect metadata for this dataset
        metadata = self._get_dataset_metadata(directory_path, dataset_parent_id=dataset_parent_id)
        logging.info("Dataset metadata: %s" % metadata)
        
        # submit workflow to publish Dataset and Files
        if self._has_data_files(directory_path):
            self._wmgr_client.uploadDataset(metadata, newVersion=False, inPlace=True, debug=True) # FIXME
            
        else:
            metadata['id'] = metadata['DatasetId']
            del metadata['DatasetId']
            self._solr_client.post(metadata, "datasets")
        
        
        # recursion into sub-directories
        for subdir_name in os.listdir(directory_path):
            subdir_path = os.path.join(directory_path, subdir_name)
            if os.path.isdir(subdir_path):
                self.crawl(subdir_path, dataset_parent_id=metadata['id'])
            
            
    def _get_dataset_metadata(self, directory_path, dataset_parent_id=None):
        '''
        Collects metadata for a given directory path
        '''
        
        (parent_path, this_dir_name) = os.path.split(directory_path)
        print("this_dir_name=%s" % this_dir_name)
                
        # read metadata from configuration files, if found
        metadata = self._read_metadata(directory_path)
        
        # use default metadata values if not populated from configuration
        if not metadata.get("CollectionId", None):
            metadata["CollectionId"] = self._collection_id
        if not metadata.get("CollectionName", None):
            metadata["CollectionName"] = self._collection_name
        if not metadata.get("DatasetParentId", None):
            metadata["DatasetParentId"] = dataset_parent_id
        if not metadata.get("DatasetName", None):
            metadata["DatasetName"] = this_dir_name
        if not metadata.get("DatasetId", None):
            if metadata.get("DatasetParentId",None):
                metadata["DatasetId"] = metadata["DatasetParentId"] + "/" + this_dir_name.lower().replace(" ","_")
            else:
                metadata["DatasetId"] = metadata["CollectionId"]  + "/" + this_dir_name.lower().replace(" ","_")
        if not metadata.get("DatasetVersion", None):
            metadata['DatasetVersion'] = 1
        
        return metadata
        
    def _read_metadata(self, directory_path):
        '''
        Reads metadata from all configuration files found in a directory.
        '''
        
        metadata = {}
        
        for config_file_name in [file_name for file_name in os.listdir(directory_path) if file_name.endswith(".cfg")]:
            
            config_file_path = os.path.join(directory_path, config_file_name)
            logging.debug("Reading configuration file: %s" % config_file_path)
            
            config = ConfigParser.ConfigParser()
            # must set following line explicitly to preserve the case of configuration keys
            config.optionxform = str 
            
            try:
                config.read(config_file_path)
                for section in config.sections():
                    for key, value in config.items(section):
                        # must escape XML reserved characters: &<>"
                        evalue = escape(value)
                        logging.debug('\t%s = %s' % (key, evalue))
                        # [Dataset] section: prefix all fields with 'Dataset:'
                        if section=='Dataset' and not key.startswith('Dataset'):
                            metadata['Dataset:%s' % key] = evalue
                        else:
                            metadata[key] = evalue
            except Exception as e:
                logging.error("ERROR reading metadata configuration")
                logging.error(e)
                sys.exit(-1)
        
        return metadata
    
    def _has_data_files(self, directory_path):
        '''
        Checks wether a dataset directoy contains data files to be published.
        '''
        
        data_files = [f for f in os.listdir(directory_path) if os.path.isfile(
            os.path.join(directory_path, f)) and not f.endswith(".cfg")]
        return len(data_files) > 0

if __name__ == '__main__':
    
    # FIXME: use argparse
    collection_name = 'COH Data Collection'
    
    labcasDatasetPublisher = LabcasDatasetPublisher(collection_name)
    
    dataset_dir = sys.argv[1]
    dataset_parent_id = None
    labcasDatasetPublisher.crawl(dataset_dir, dataset_parent_id=dataset_parent_id)
    

