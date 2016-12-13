# Example Python script to execute the labcas-upload workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
        
    title = 'IPMN Lesions'
    productType = title.replace(' ','_')
    datasetId = '2016_I_NCIU01' # must match directory name in $LABCAS_STAGING
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
    metadata = { 'Title':title,
                 'Description':'Lesions of Intraductal Papillary Mucinous Neoplasm',
                 #'Consortium':'MCL',
                 #'Discipline':'Pathology',
                 #'LeadPI':'Maitra Anirban',
                 #'OwnerPrincipal':'uid=anirban,dc=edrn,dc=jpl,dc=nasa,dc=gov',
                 #'QAState':'Public',
                 #'Organ':'Pancreas',
                 #'Institution':'MD Anderson Cancer Center',
                 #'ImagingTechnique':'TMA (Tissue Micro Array)',
                 #'Project':'N/A',
                 #'Species':'Human' 
               }
    

    # upload dataset staged in directory 'mydatadir'
    labcasClient.uploadCollection(datasetId, metadata)

    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(productType)
    
    # or equivalently
    labcasClient.getProductTypeById("urn:edrn:%s" % productType)
    
    # list all products for given product type
    labcasClient.listProducts(productType)
