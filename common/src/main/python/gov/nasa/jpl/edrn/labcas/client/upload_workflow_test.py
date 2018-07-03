# Example script to execute the "labcas-test" workflow
# python gov/nasa/jpl/edrn/labcas/client/upload_workflow_test.py <in_place>
# where in_place = True/False

from gov.nasa.jpl.edrn.labcas.client.workflow_client import WorkflowManagerClient
import logging
import sys
from gov.nasa.jpl.edrn.labcas.utils import str2bool
logging.basicConfig(level=logging.INFO)

if __name__ == '__main__':
    
    in_place = str2bool(sys.argv[1])
    
    # submit workflow
    # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName labcas-test --metaData --key experiment 11 --key species snakes
    more_metadata = {'DatasetName': 'Dataset_1', 
                     'CollectionName': 'My Data Collection', 
                     'CollectionId': 'My_Data_Collection', 
                     'DatasetParentId': None, 
                     'DatasetVersion': 1, 
                     'DatasetId': 'My_Data_Collection/dataset_1',
                     'UpdateCollection': 'true',
                     'UpdateDataset': 'true'
    }
    minimal_metadata = {
        'CollectionName': 'My Data Collection',
        'DatasetName': 'Dataset 1'
    }
    wmgrClient = WorkflowManagerClient()
    
    if in_place:
        logging.info("Publishing data in place (aka without moving the files)")
        wInstId = wmgrClient.executeWorkflow(['urn:edrn:LabcasUploadInitTask','urn:edrn:LabcasUpload2ExecuteTask'], 
                                             minimal_metadata)
    else:
        logging.info("Publishing data while moving files to the archive")
        wInstId = wmgrClient.executeWorkflow(['urn:edrn:LabcasUploadInitTask','urn:edrn:LabcasUploadExecuteTask'], 
                                             minimal_metadata)

    # monitor workflow instance
    wmgrClient.waitForCompletion(wInstId)

    # list all product types
    wmgrClient.listProductTypes()
    
    # list all files of this product type
    wmgrClient.listProducts('My_Data_Collection')
