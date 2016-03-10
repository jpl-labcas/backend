# Example Python script to execute the labcas-upload workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
        
    datasetId = 'mydata'
    labcasClient = LabcasClient()
    
    # print out workflow definition
    #labcasClient.getWorkflowsByEvent("labcas-upload")
    # or equivalently
    labcasClient.getWorkflowById("urn:edrn:LabcasUploadWorkflow")
    
    # required metadata fields
    #./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName labcas-upload --metaData 
    # --key DatasetId mydata --key DatasetName 'My Data' --key Description 'My own data' 
    # --key ProtocolId 1 --key LeadPI 'John Doe' --key ProtocolName 'GSTP1 Methylation' 
    # --key DataCustodian 'Rich Smith' --key DataCustodianEmail 'rich.smith@pubmed.gov' --key CollaborativeGroup 'Prostate and Urologic'
    metadata = { 'Description':'My own data',
                 'DatasetName':'My Data',
                 'ProtocolId':'1',
                 'ProtocolName':'GSTP1 Methylation',
                 'LeadPI':'John Doe',
                 'DataCustodian':'Rich Smith',
                 'DataCustodianEmail':'rich.smith@pubmed.gov',
                 'CollaborativeGroup':'Prostate and Urologic',
                 'OwnerGroup':'EDRN_CANCER_GROUP',
    } 
    

    # upload dataset staged in directory 'mydata'
    labcasClient.uploadDataset(datasetId, metadata)

    # update the dataset metadata WITHOUT generating a new version
    metadata['ProtocolId'] = '99'
    labcasClient.uploadDataset(datasetId, metadata)
    
    # update dataset metadata while generating a new version
    metadata['LeadPI'] = 'Mister X'
    labcasClient.uploadDataset(datasetId, metadata, newVersion=True)

    # list all product types in File manager
    labcasClient.listProductTypes()
    
    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(datasetId)
    
    # or equivalently
    labcasClient.getProductTypeById("urn:edrn:%s" % datasetId)
    
    # list all products for given dataset == product type
    labcasClient.listProducts(datasetId)