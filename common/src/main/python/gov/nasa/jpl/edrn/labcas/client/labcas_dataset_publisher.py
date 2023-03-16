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
    
    def __init__(self, 
                 collection_name, solr_url='https://localhost:8984/solr', 
                 workflow_url='http://localhost:9001', update_collection=True):

        self._collection_name = collection_name
        # NOTE: "CollectionId" must match directory location 
        # under $LABCAS_ARCHIVE or $LABCAS_STAGING
        self._collection_id = collection_name.replace(" ", "_")
        
        # flag to pass in "UpdateCollection"=true to the Workflow Manager
        # (on first invocation only)
        self._update_collection = update_collection
        
        self._solr_client = SolrClient(solr_url)
        self._wmgr_client = WorkflowManagerClient(workflowManagerUrl=workflow_url)
            
    def crawl(self, directory_path, dataset_parent_id=None, in_place=True, 
              update_datasets=True, update_files=True):
        '''
        Recursively parses a directory path and publishes all datasets.
        '''
        
        logging.info("Crawling directory: %s" % directory_path)
        
        # collect metadata for this dataset
        metadata = self._get_dataset_metadata(directory_path, dataset_parent_id=dataset_parent_id)
        # FIXME: inject metadata from the client side
        # metadata['DatasetSequenceType'] = 'MIP'
        logging.info("Dataset metadata: %s" % metadata)
        
        # update dataset metadata
        if update_datasets:
            # modify a copy of the dictionary
            _metadata = metadata.copy()
            _metadata['id'] = _metadata['DatasetId']
            del _metadata['DatasetId']
            self._solr_client.post(_metadata, "datasets")

        # submit workflow to publish Dataset and Files
        if self._has_data_files(directory_path) and update_files:
            if not self._update_collection:
                metadata['UpdateCollection'] = "false"
            if not update_datasets:
                metadata['UpdateDataset'] = "false"
            self._wmgr_client.uploadDataset(metadata, 
                                            update_dataset=update_datasets, 
                                            in_place=in_place, 
                                            debug=False)
            # do not update the collection metadata more than once
            self._update_collection = False            
        
        # recursion into sub-directories
        for subdir_name in os.listdir(directory_path):
            subdir_path = os.path.join(directory_path, subdir_name)
            if os.path.isdir(subdir_path):
                self.crawl(subdir_path, 
                           dataset_parent_id=metadata['DatasetId'], 
                           update_files=update_files)
            
            
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
        # build the DatasetId from the CollectionId, DatasetParentId and local directory
        if not metadata.get("DatasetId", None):
            if metadata.get("DatasetParentId", None):
                metadata["DatasetId"] = metadata["DatasetParentId"] + "/" + this_dir_name
            else:
                metadata["DatasetId"] = metadata["CollectionId"]  + "/" + this_dir_name
        if not metadata.get("DatasetVersion", None):
            metadata['DatasetVersion'] = 1
        
        return metadata
            
    def _has_data_files(self, directory_path):
        '''
        Checks wether a dataset directoy contains data files to be published.
        '''
        
        data_files = [f for f in os.listdir(directory_path) if os.path.isfile(
            os.path.join(directory_path, f)) and not f.endswith(".cfg") and not f.endswith(".met") and not f.endswith(".xmlmet")]
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
    parser.add_argument('--solr_url', type=str, default='http://localhost:8983/solr',
                        help='URL of Solr Index')
    parser.add_argument('--workflow_url', type=str, default='http://localhost:9001',
                        help='URL of Workflow Manager XML/RPC server')
    args_dict = vars( parser.parse_args() )
        
    # start publishing
    labcasDatasetPublisher = LabcasDatasetPublisher(args_dict['collection_name'], 
                                                    update_collection=args_dict['update_collection'],
                                                    solr_url=args_dict['solr_url'],
                                                    workflow_url=args_dict['workflow_url'])
    labcasDatasetPublisher.crawl(args_dict['dataset_dir'], 
                                 dataset_parent_id=args_dict['dataset_parent_id'],
                                 in_place=args_dict['in_place'])
    

