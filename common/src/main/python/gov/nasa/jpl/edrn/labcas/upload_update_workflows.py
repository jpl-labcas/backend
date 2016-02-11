# Example Pythin script to execute the labcas-upload and labcas-update workflows

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
        
    dataset = 'mydata'
    labcasClient = LabcasClient()
    
    # print out workflow definition
    #labcasClient.getWorkflowsByEvent("labcas-upload")
    # or equivalently
    labcasClient.getWorkflowById("urn:edrn:LabcasUploadWorkflow")
    
    # core metadata fields
    metadata = { 'Description':'My own data',
                 'DatasetName':'My Data',
                 'ProtocolId':'99',
                 'LeadPI':'John Doe'} 
    

    # upload dataset staged in directory 'mydata'
    labcasClient.uploadDataset(dataset, metadata)
'''
    # upload the dataset again:
    # o a new version will be generated
    # o the product type metadata will be completey overridden
    metadata['ProtocolId'] = '98'
    #labcasClient.uploadDataset(dataset, metadata)

 
    # update dataset metadata - no new version is generated
    metadata['LeadPI'] = 'Mister X'
    #labcasClient.updateDataset(dataset, metadata)
    
    # list all product types in File manager
    labcasClient.listProductTypes()
    
    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(dataset)
    
    # or equivalently
    labcasClient.getProductTypeById("urn:edrn:%s" % dataset)
    
    # list all products for given dataset == product type
    labcasClient.listProducts(dataset)
'''