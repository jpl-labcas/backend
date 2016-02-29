# Example script to execute a NIST Workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # submit workflow
    # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName nist 
    #               --metaData --key DatasetId NIST_LAB01 --key LeadPI Johns --key Instrument trombone --key Lab LAB01 --key Date 20160101
    labcasClient = LabcasClient()
    wInstId = labcasClient.executeWorkflow(['urn:edrn:NistInitTask','urn:edrn:NistConvertTask','urn:edrn:NistExecTask','urn:edrn:NistCrawlTask'], 
                                           {'DatasetId':'NIST_LAB01',
                                            'LeadPI':'Johns', 
                                            'Instrument':'trumpet', 
                                            'Lab':'LAB01',
                                            'Date':'20160101' } )
    # monitor workflow instance
    labcasClient.waitForCompletion(wInstId)
