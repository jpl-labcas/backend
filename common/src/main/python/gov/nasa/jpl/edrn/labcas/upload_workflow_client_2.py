# Example Python script to execute the labcas-upload workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
        
    collection_name = 'IPMN Lesions'
    collection_description = 'Lesions of Intraductal Papillary Mucinous Neoplasm'
    dataset_name = 'P1-06_H03'
    #dataset_name = '2016_I_NCIU01'
    owner_principal = 'uid=anirban,dc=edrn,dc=jpl,dc=nasa,dc=gov'
    dataset_description = 'Lesions images'

    product_type = collection_name.replace(' ','_')
    dataset_id = dataset_name.replace(' ','_') # must match directory name in $LABCAS_STAGING/<product_type>
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
                 'CollectionName': collection_name,
                 'CollectionDescription': collection_description,
                 'DatasetName': dataset_name,
                 'OwnerPrincipal': owner_principal,
                 'DatasetDescription': dataset_description,

                 # other metadata
                 'Consortium': 'MCL',
                 'Discipline': 'Pathology',
                 'LeadPI': 'Maitra Anirban',
                 'QAState': 'Public',
                 'Organ': 'Pancreas',
                 'Institution': 'MD Anderson Cancer Center',
                 'ImagingTechnique': 'TMA (Tissue Micro Array)',
                 'Project': 'N/A',
                 'Species':'Human' 

               }
    

    # upload dataset staged in directory 'mydatadir'
    labcasClient.uploadCollection(dataset_name, metadata)

    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(product_type)
    
    # or equivalently
    labcasClient.getProductTypeById("urn:edrn:%s" % product_type)
    
    # list all products for given product type
    labcasClient.listProducts(product_type)
