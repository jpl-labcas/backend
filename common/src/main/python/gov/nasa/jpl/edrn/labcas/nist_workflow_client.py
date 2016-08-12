# Example script to execute a NIST Workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # submit workflow
    # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName nist 
    #               --metaData --key DatasetId Lab005_C_R03 --key LabNumber 005 
    #               --key NormalizationMethod C --key RoundNumber 003 --key LeadPI Johns 
    #               --key DataCollectionDate 20160101 --key SampleProcessingProtocols 'With water and ammonia' 
    #               --key InstrumentationTechnologyCode NGS --key Manufacturer TexasInstruments 
    #               --key ModelNumber XYZ123 --key DataProcessingProtocols 'Crunching data' 
    #               --key OwnerGroup Lab005_OwnerPrincipal
    labcasClient = LabcasClient()
    wInstId = labcasClient.executeWorkflow(['urn:edrn:NistInitTask',
                                            'urn:edrn:NistConvertTask',
                                            'urn:edrn:NistExecTask',
                                            #'urn:edrn:NistExec2Task',
                                            'urn:edrn:NistCrawlTask'], 
                                           {'DatasetId':'Lab005_C_R03',
                                            'LabNumber':'005',
                                            'NormalizationMethod':'C',
                                            'RoundNumber':'003',
                                            'LeadPI':'Johns', 
                                            'DataCollectionDate':'20160101',
                                            'SampleProcessingProtocols':'With water and ammonia',
                                            'InstrumentationTechnologyCode':'NGS', 
                                            'Manufacturer':'TexasInstruments',
                                            'ModelNumber':'XYZ123',
                                            'DataProcessingProtocols':'Crunching data',
                                            'OwnerPrincipal':'Lab005' } )
    # monitor workflow instance
    labcasClient.waitForCompletion(wInstId)
