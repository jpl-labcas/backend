# Example Python script to execute the labcas-upload workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # required input metadata    
    collection_name = 'My Data Collection'
    collection_description = 'This is my precious data collection'
    dataset_name = 'Best Dataset'
    dataset_description = 'The Best Dataset of this collection'
    owner_principal = 'uid=testuser,dc=edrn,dc=jpl,dc=nasa,dc=gov',
    
    # NOTE: data must be uploaded to directory $LABCAS_STAGING/<product_type>/<dataset_id>
    product_type = collection_name.replace(' ','_')
    #dataset_id = dataset_name.replace(' ','_') 
    labcasClient = LabcasClient()
    
    # print out workflow definition
    #labcasClient.getWorkflowsByEvent("labcas-upload")
    # or equivalently
    labcasClient.getWorkflowById("urn:edrn:LabcasUploadWorkflow")
    
    # required metadata fields
    #./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName labcas-upload --metaData 
    # --key DatasetId mydata --key ProductType 'MyData' --key Description 'My own data' 
    # --key ProtocolId 1 --key LeadPI 'John Doe' --key ProtocolName 'GSTP1 Methylation' --key OrganSite Lung --key OwnerPrincipal EDRN_CANCER_GROUP
    # --key DataCustodian 'Rich Smith' --key DataCustodianEmail 'rich.smith@pubmed.gov' --key CollaborativeGroup 'Prostate and Urologic'
    metadata = { # required metadata
                 'CollectionName':collection_name,
                 'CollectionDescription':collection_description,
                 'DatasetName': dataset_name,
                 'OwnerPrincipal': owner_principal,
                 'DatasetDescription': dataset_description,
                 
                 # other metadata
                 'ProtocolId':'1',
                 'ProtocolName':'GSTP1 Methylation',
                 'LeadPI':'John Doe',
                 'DataCustodian':'Ed Stark',
                 'DataCustodianEmail':'rich.smith@pubmed.gov',
                 'CollaborativeGroup':'Prostate and Urologic',
                 'OrganSite':'Pancreas',
                 'UpdateCollection':'true', # default value = true
                 
    } 
    

    # upload dataset staged in directory $LABCAS_STAGING/<product_type>/<dataset_id>
    labcasClient.uploadCollection(dataset_name, metadata)

    # update the dataset metadata WITHOUT generating a new version
    metadata['ProtocolId'] = '99'
    labcasClient.uploadCollection(dataset_name, metadata)
    
    # update dataset metadata while generating a new version
    metadata['LeadPI'] = 'Mister X'
    labcasClient.uploadCollection(dataset_name, metadata, newVersion=True)

    # list all product types in File manager
    labcasClient.listProductTypes()
    
    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(product_type)
    
    # or equivalently
    labcasClient.getProductTypeById("urn:edrn:%s" % product_type)
    
    # list all products for given product type
    labcasClient.listProducts(product_type)
