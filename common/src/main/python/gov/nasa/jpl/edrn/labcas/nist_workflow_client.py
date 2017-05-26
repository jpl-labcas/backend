# Example script to execute a NIST Workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # submit workflow
    # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName nist 
    #               --metaData --key DatasetName Lab005_C_R03 --key LabNumber 005 
    #               --key NormalizationMethod C --key RoundNumber 03 --key LeadPI Johns 
    #               --key DataCollectionDate 20160101 --key SampleProcessingProtocols 'With water and ammonia' 
    #               --key InstrumentationTechnologyCode NGS --key Manufacturer TexasInstruments 
    #               --key ModelNumber XYZ123 --key DataProcessingProtocols 'Crunching data' 
    #               --key OwnerGroup Lab005_OwnerPrincipal
    labcasClient = LabcasClient()

    workflowTasks = ['urn:edrn:NistInitTask',
                     'urn:edrn:NistConvertTask',
                     'urn:edrn:NistExecTask',
                     'urn:edrn:NistCrawlTask']
    
    metadata = {'DatasetId':'Lab008_NGS004_NIST03',
                'DatasetName':'Lab008 NGS004 NIST03',
                'LabNumber':'Lab008',
                'ProtocolName':'NGS004',
                'SampleId':'NIST03',
                'DataCollectionDate':'20160101',
                'NewVersion':'false',
                'UpdateCollection':'false' }

    # upload dataset without changing the version
    wInstId = labcasClient.executeWorkflow(workflowTasks, metadata)
    # monitor workflow instance
    labcasClient.waitForCompletion(wInstId)

    # upload new version of same dataset, fix MetaData
    #metadata['NewVersion']='true'
    #metadata['LeadPI']='Pine, Scott'
    #wInstId = labcasClient.executeWorkflow(workflowTasks, metadata)
    # monitor workflow instance
    #labcasClient.waitForCompletion(wInstId)
