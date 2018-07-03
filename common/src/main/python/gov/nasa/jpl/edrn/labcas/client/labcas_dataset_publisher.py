import argparse
import logging
import os
import sys
import urllib2

from gov.nasa.jpl.edrn.labcas.client.metadata_utils import read_config_metadata
from gov.nasa.jpl.edrn.labcas.client.solr_client import SolrClient
from gov.nasa.jpl.edrn.labcas.client.workflow_client import WorkflowManagerClient
from gov.nasa.jpl.edrn.labcas.utils import str2bool

logging.basicConfig(level=logging.DEBUG)


class LabcasDatasetPublisher(object):
    '''
    Publishes a hierarchy of datasets rooted at some directory.
    '''
    
    def __init__(self, collection_name, solr_url='http://localhost:8983/solr', update_collection=True):
        
        
        self._collection_name = collection_name
        # NOTE: "CollectionId" must match directory location 
        # under $LABCAS_ARCHIVE or $LABCAS_STAGING
        self._collection_id = collection_name.replace(" ", "_")
        
        # flag to pass in "UpdateCollection"=true to the Workflow Manager
        # (on first invocation only)
        self._update_collection = update_collection
        
        self._solr_client = SolrClient(solr_url)
        self._wmgr_client = WorkflowManagerClient()
        
            
    def crawl(self, directory_path, dataset_parent_id=None, in_place=True, 
              update_datasets=True, update_files=True):
        '''
        Recursively parses a directory path and publishes all datasets.
        '''
        
        logging.info("Crawling directory: %s" % directory_path)
        
        # collect metadata for this dataset
        metadata = self._get_dataset_metadata(directory_path, dataset_parent_id=dataset_parent_id)
        logging.info("Dataset metadata: %s" % metadata)
        dataset_id =  metadata['DatasetId']
        
        # submit workflow to publish Dataset and Files
        if update_files:
            if self._has_data_files(directory_path):
                if not self._update_collection:
                    metadata['UpdateCollection'] = "false"
                self._wmgr_client.uploadDataset(metadata, 
                                                update_dataset=update_datasets, 
                                                in_place=in_place, 
                                                debug=False)
                # do not update the collection metadata more than once
                self._update_collection = False
            
        else:
            if update_datasets:
                metadata['id'] = dataset_id
                del metadata['DatasetId']
                self._solr_client.post(metadata, "datasets")
        
        # recursion into sub-directories
        for subdir_name in os.listdir(directory_path):
            subdir_path = os.path.join(directory_path, subdir_name)
            if os.path.isdir(subdir_path):
                self.crawl(subdir_path, dataset_parent_id=dataset_id)
            
            
    def _get_dataset_metadata(self, directory_path, dataset_parent_id=None):
        '''
        Collects metadata for a given directory path
        '''
        
        # remove last '/' otherwise the path is not split correctly
        if directory_path.endswith('/'):
            directory_path = directory_path[:-1]
        (parent_path, this_dir_name) = os.path.split(directory_path)
                
        # read metadata from configuration files, if found
        metadata = read_config_metadata(directory_path)
        
        # use default metadata values if not populated from configuration
        if not metadata.get("CollectionId", None):
            metadata["CollectionId"] = self._collection_id
        if not metadata.get("CollectionName", None):
            metadata["CollectionName"] = self._collection_name
        if not metadata.get("DatasetParentId", None):
            if dataset_parent_id:
                metadata["DatasetParentId"] = dataset_parent_id
        if not metadata.get("DatasetName", None):
            metadata["DatasetName"] = this_dir_name
        # build the DatasetId from the CollectionId or the DatasetParentId
        if not metadata.get("DatasetId", None):
            if metadata.get("DatasetParentId", None):
                metadata["DatasetId"] = metadata["DatasetParentId"] + "/" + this_dir_name.lower().replace(" ","_")
            else:
                metadata["DatasetId"] = metadata["CollectionId"]  + "/" + this_dir_name.lower().replace(" ","_")
        if not metadata.get("DatasetVersion", None):
            metadata['DatasetVersion'] = 1
        
        return metadata
            
    def _has_data_files(self, directory_path):
        '''
        Checks wether a dataset directoy contains data files to be published.
        '''
        
        data_files = [f for f in os.listdir(directory_path) if os.path.isfile(
            os.path.join(directory_path, f)) and not f.endswith(".cfg")]
        return len(data_files) > 0

if __name__ == '__main__':
    
    # parse command line arguments
    parser = argparse.ArgumentParser()
    parser.add_argument('--dataset_dir', type=str, help='Dataset root directory')
    parser.add_argument('--collection_name', type=str, 
                        help='Collection name (matching the collection root directory)')
    parser.add_argument('--dataset_parent_id', type=str, default=None,
                        help='Optional parent dataset id')
    parser.add_argument('--in_place', type=str2bool, default=True,
                        help='Optional flag to publish data without moving it (default: True)')
    parser.add_argument('--update_collection', type=str2bool, default=True,
                        help='Optional flag to update the collection metadata when publishing files (default: True)')
    args_dict = vars( parser.parse_args() )
        
    # start publishing
    labcasDatasetPublisher = LabcasDatasetPublisher(args_dict['collection_name'], 
                                                    update_collection=args_dict['update_collection'])
    labcasDatasetPublisher.crawl(args_dict['dataset_dir'], 
                                 dataset_parent_id=args_dict['dataset_parent_id'],
                                 in_place=args_dict['in_place'])
    

