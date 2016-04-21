# Example script to query for a product type

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    labcasClient = LabcasClient()
    
    # retrieve a specific product type
    #labcasClient.getProductTypeByName("RnaSeqProduct")
    #labcasClient.getProductTypeByName("ERR164552")
    
    # list top-level product types
    topLevelProductTypes = labcasClient.listTopLevelProductTypes()
    print topLevelProductTypes
    # list children product types
    for parentDatasetId in topLevelProductTypes.keys():
        childrenDatasetIds =  labcasClient.listProductTypesByParent(parentDatasetId)
        print 'parent=%s children=%s' % (parentDatasetId, childrenDatasetIds)