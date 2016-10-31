# Example script to submit a 'rnaseq-pipeline' workflow

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # submit workflow
    # ./wmgr-client --url http://localhost:9001 --operation --sendEvent --eventName rnaseq --metaData --key DatasetId ERR164503 --key Title LC_C1 --key AgeAtDiagnosis 45 --key Gender male --key SmokingStatus true --key Stage 1A --key ReferenceId geo:GSM993606 --key ReferenceUrl 'http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSM993606' --key OrganSite Lung --key OwnerPrincipal RnaResearchGroup --key GenomeIndex genome
    labcasClient = LabcasClient()
    wInstId = labcasClient.executeWorkflow(['urn:edrn:RnaSeqInitTask',
                                            'urn:edrn:RnaSequenceTask1',
                                            'urn:edrn:RnaSequenceTask2',
                                            'urn:edrn:RnaSequenceTask3',
                                            'urn:edrn:RnaSeqCopyTask',
                                            'urn:edrn:RnaSeqCrawlTask'], 
                                           {'DatasetId':'ERR164503',
                                            'Title':'LC_C1', 
                                            'AgeAtDiagnosis':'45',
                                            'Gender':'male',
                                            'SmokingStatus':'true',
                                            'Stage':'1A',
                                            'ReferenceId':'geo:GSM993606',
                                            'ReferenceUrl':'http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSM993606',
                                            'OrganSite':'Lung',
                                            'GenomeIndex':'genome',
                                            'OwnerPrincipal':'RnaResearchGroup' } )
    # monitor workflow instance
    labcasClient.waitForCompletion(wInstId)
