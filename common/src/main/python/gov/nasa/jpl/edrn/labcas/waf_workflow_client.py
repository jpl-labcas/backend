# Python script for programmatic execution of the AF workflow
# (and consequent publishing of UnivColoLungImages)

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':

    # upload these datasets    
    #datasetIds = ['UCHSC_1467', 'UCHSC_8798']
    datasetIds = ['UCHSC_1467']
    productType = 'University_of_Colorado_Lung_Image'
    
    for datasetId in datasetIds:
    
        # submit workflow
        # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName waf --metaData --key DatasetId UCHSC_1001
        labcasClient = LabcasClient()
        wInstId = labcasClient.executeWorkflow(['urn:edrn:WafInitTask',
                                                'urn:edrn:WafCrawlTask'], 
                                               { 'DatasetId':datasetId },
                                               )
        # monitor workflow instance
        labcasClient.waitForCompletion(wInstId)
        
        # list all products for given product type
        labcasClient.listProducts(productType)
