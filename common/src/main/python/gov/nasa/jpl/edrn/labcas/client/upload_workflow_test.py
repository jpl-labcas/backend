# Example script to execute the "labcas-test" workflow

from gov.nasa.jpl.edrn.labcas.client.workflow_client import WorkflowManagerClient
import logging
logging.basicConfig(level=logging.INFO)

if __name__ == '__main__':
    
    # submit workflow
    # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName labcas-test --metaData --key experiment 11 --key species snakes
    more_metadata = {'DatasetName': 'Dataset_1', 
                     'CollectionName': 'My Data Collection', 
                     'CollectionId': 'My_Data_Collection', 
                     'DatasetParentId': None, 
                     'DatasetVersion': 1, 
                     'DatasetId': 'My_Data_Collection/dataset_1'
    }
    minimal_metadata = {
        'CollectionName': 'My Data Collection',
        'DatasetName': 'Dataset 1'
    }
    wmgrClient = WorkflowManagerClient()
    wInstId = wmgrClient.executeWorkflow(['urn:edrn:LabcasUploadInitTask','urn:edrn:LabcasUploadExecuteTask'], 
                                         minimal_metadata)

    # monitor workflow instance
    wmgrClient.waitForCompletion(wInstId)

    # list all product types
    wmgrClient.listProductTypes()
    
    # list all files of this product type
    wmgrClient.listProducts('My_Data_Collection')
