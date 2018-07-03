# Example script to execute the "labcas-test" workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # submit workflow
    # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName labcas-test --metaData --key experiment 11 --key species snakes
    labcasClient = LabcasClient()
    wInstId = labcasClient.executeWorkflow(['urn:edrn:LabcasTestInit','urn:edrn:LabcasTestTask'], 
                                           {'experiment':'11', 
                                            'species':'snakes' } )

    # monitor workflow instance
    labcasClient.waitForCompletion(wInstId)

    # list all files of this product type
    labcasClient.listProducts('LabCAS_Test_Product')
