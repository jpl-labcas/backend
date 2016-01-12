# Example script to execute a NIST Workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # submit workflow
    # /wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName nist --metaData --key pi Johns --key instrument trombone --key lab LAB01 --key date 20160101 --key mirnadatadir nist/LAB01/20160101
    labcasClient = LabcasClient()
    wInstId = labcasClient.executeWorkflow(['urn:edrn:NistInitTask','urn:edrn:NistConvertTask','urn:edrn:NistExecuteTask'], 
                                           {'pi':'Johns', 
                                            'instrument':'trumpet', 
                                            'lab':'LAB01','date':'20160101', 
                                            'mirnadatadir':'nist/LAB01/20160101' } )

    # monitor workflow instance
    labcasClient.waitForCompletion(wInstId)
