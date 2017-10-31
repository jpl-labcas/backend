# Example script to execute a NIST Workflow

# NOTE: before runningthe NISTProduct collection must exist in Solr. To create:
# cd $LABCAS_HOME/cas-filemgr
# java -Djava.ext.dirs=lib gov.nasa.jpl.edrn.labcas.utils.SolrUtils $LABCAS_HOME/workflows/nist
#
# To ingest a single NIST dataset:
# o place data file in directory $LABCAS_STAGING/NIST_Product/Lab008_NGS004_NIST03/Lab008_NGS004_NIST03.txt
# o cd labcas-backend/common/src/main/python
# o python gov/nasa/jpl/edrn/labcas/client/nist_workflow_client.py


from labcas_client import LabcasClient

def publish_nist_dataset(metadata):
    
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
                     'urn:edrn:NistExecTask',
                     'urn:edrn:NistCrawlTask']
    

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
    
if __name__ == '__main__':
    
    DatasetIds = ['Lab004_PCR004_NIST03', 'Lab004_PCR004_NIST04', 'Lab004_PCR004_NIST05',
                  'Lab005_HYB001_NIST03', 'Lab005_HYB001_NIST04', 'Lab005_HYB001_NIST05',
                  'Lab006_NGS001_NIST03', 'Lab006_NGS001_NIST04', 'Lab006_NGS002_NIST03', 'Lab006_NGS002_NIST04', 'Lab006_NGS002_NIST05',
                  'Lab007_NGS003_NIST03', 'Lab007_NGS003_NIST04', 'Lab007_NGS003_NIST05',
                  'Lab008_NGS004_NIST03', 'Lab008_NGS004_NIST04', 'Lab008_NGS004_NIST05']
    
    for DatasetId in DatasetIds:
        (LabNumber, ProtocolName, SampleId) = DatasetId.split("_")
    
        metadata = {'DatasetId': DatasetId,
                    'DatasetName':DatasetId.replace("_"," "),
                    'LabNumber':LabNumber,
                    'ProtocolName':ProtocolName,
                    'SampleId':SampleId,
                    'DataCollectionDate':'20160101',
                    'NewVersion':'false',
                    'UpdateCollection':'false' }
    
        publish_nist_dataset(metadata)
