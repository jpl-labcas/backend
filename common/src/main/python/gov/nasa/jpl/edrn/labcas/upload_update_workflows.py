# Example Pythin script to execute the labcas-upload and labcas-update workflows

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
        
    dataset = 'mydata'
    labcasClient = LabcasClient()
    
    # upload dataset staged in directory 'mydata'
    labcasClient.uploadDataset(dataset)
    
    # upload the dataset again:
    # o a new version will be generated
    # o the product type metadata will be completey overridden
    labcasClient.uploadDataset(dataset)
    
    # update dataset metadata - no new version is generated
    labcasClient.updateDataset(dataset)
    
    # list all product types in File manager
    labcasClient.listProductTypes()
    
    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(dataset)
    # or equivalently
    labcasClient.getProductTypeById("urn:edrn:%s" % dataset)
    
    # list all products for given dataset == product type
    labcasClient.listProducts(dataset)