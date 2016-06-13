# Example script to submit a 'rnaseq-pipeline' workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # submit workflow
    # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName rnaseq-pipeline 
    #               --metaData --key genome_index genome --key sample_id ERR318895 --key OwnerGroup RnaResearchGroup
    labcasClient = LabcasClient()
    wInstId = labcasClient.executeWorkflow(['urn:edrn:RnaSequencePipelineInit',
                                            'urn:edrn:RnaSequencePipelineTask1',
                                            'urn:edrn:RnaSequencePipelineTask2',
                                            'urn:edrn:RnaSequencePipelineTask3'], 
                                           {'genome_index':'genome',
                                            'sample_id':'ERR164503', 
                                            'OwnerGroup':'RnaResearchGroup' } )
    # monitor workflow instance
    labcasClient.waitForCompletion(wInstId)
