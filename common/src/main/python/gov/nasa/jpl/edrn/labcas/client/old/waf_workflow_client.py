# Python script for programmatic execution of the AF workflow
# (and consequent publishing of UnivColoLungImages)

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':

    # upload these datasets    
    #dataset_names = ['UCHSC_1467', 'UCHSC_8798']
    #dataset_names = ['UCHSC_1467']
    dataset_names = ['UCHSC_8798']
    product_type = 'University_of_Colorado_Lung_Image'
    
    for dataset_name in dataset_names:
    
        # submit workflow
        # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName waf --metaData --key DatasetId UCHSC_1001
        labcasClient = LabcasClient()
        wInstId = labcasClient.executeWorkflow(['urn:edrn:WafInitTask',
                                                'urn:edrn:WafCrawlTask'], 
                                               { 'DatasetName':dataset_name },
                                               )
        # monitor workflow instance
        labcasClient.waitForCompletion(wInstId)
        
        # list all products for given product type
        labcasClient.listProducts(product_type)
