# Python script to migrate all ECAS data to LabCAS

import os
from xml.etree import ElementTree as ET
import pprint

ecas_metadata_dir = "/Users/cinquini/workbench/ecas_to_labcas/datasets"
labcas_metadata_dir = "/Users/cinquini/eclipse-workspace/ecas-metadata/"

collection_dict = {
                   "CollectionName":"",
                   "CollectionDescription":"",
                   "Discipline":"",
                   "Institution":"",
                   "InstitutionId":"",
                   "LeadPI":"",
                   "LeadPIId":"",
                   "DataCustodian":"",
                   "DataCustodianEmail":"",
                   "Organ":"",
                   "OrganId":"",
                   "OwnerPrincipal":"cn=All Users,dc=edrn,dc=jpl,dc=nasa,dc=gov",
                   "CollaborativeGroup":"",
                   "QAState":"",
                   "Consortium":"EDRN",
                   "ProtocolName":"",
                   "ProtocolId":"",
                   "Species":"",
                   "MethodDetails":"",
                   "ResultsAndConclusionSummary":"",
                   "PubMedID":"",
                   "DateDatasetFrozen":"",
                   "QAState":"",
                   "DataDisclaimer":""
                   }

dataset_dict = {
                "DatasetId":"",
                "DatasetName":"",
                "DatasetDescription":""
                }

def convert_metadata(input_xml_file):
    '''
    Reads XML metadata and converts it to Python dictionaries.
    '''
    
    print("Reading %s" % input_xml_file)
    collection_metadata = collection_dict.copy()
    dataset_metadata = dataset_dict.copy()
        
    xml = ET.parse(input_xml_file)
    root_element = xml.getroot()
    
    # Description --> CollectionDescription
    description_element = root_element.find('.//description')
    collection_metadata['CollectionDescription'] = description_element.text
    
    metadata_element = root_element.find('.//metadata')
    for metadata_element in metadata_element:
        key = metadata_element.find("key").text
        val = metadata_element.find("val").text
        # DataSetName --> CollectionName, DatasetName
        if key == 'DataSetName':
            collection_metadata['CollectionName'] = val
            dataset_metadata['DatasetName'] = val
            dataset_metadata['DatasetDescription'] = val
            dataset_metadata['DatasetId'] = val.replace(" ","_")
            
        # ProtocolID --> ProtocolId, ProtocolName
        elif key == 'ProtocolID':
            collection_metadata['ProtocolId'] = val
            collection_metadata['ProtocolName'] = "FIXME"
            
        # LeadPI --> LeadPI, LeadPIId
        elif key == 'LeadPI':
            collection_metadata['LeadPI'] = val
            collection_metadata['LeadPIId'] = "FIXME"
            
        # SiteName --> Institution, InstitutionId
        elif key == 'SiteName':
            collection_metadata['InstitutionId'] = val
            collection_metadata['Institution'] = "FIXME"
            
        # DataCustodian --> DataCustodian
        elif key == 'DataCustodian':
            collection_metadata['DataCustodian'] = val
            
        # DataCustodianEmail --> DataCustodianEmail
        elif key == 'DataCustodianEmail':
            collection_metadata['DataCustodianEmail'] = val
            
        # OrganSite --> Organ, OrganId
        elif key == 'OrganSite':
            collection_metadata['Organ'] = val
            collection_metadata['OrganId'] = "FIXME"
            
        # CollaborativeGroup --> CollaborativeGroup
        elif key == 'CollaborativeGroup':
            collection_metadata['CollaborativeGroup'] = val
            
        # MethodDetails --> MethodDetails
        elif key == 'MethodDetails':
            collection_metadata['MethodDetails'] = val
            
        # ResultsAndConclusionSummary --> ResultsAndConclusionSummary
        elif key == 'ResultsAndConclusionSummary':
            collection_metadata['ResultsAndConclusionSummary'] = val
            
        # PubMedID --> PubMedID
        elif key == 'PubMedID':
            collection_metadata['PubMedID'] = "http://www.ncbi.nlm.nih.gov/pubmed/%s" % val
            
        # DateDatasetFrozen --> DateDatasetFrozen
        elif key == 'DateDatasetFrozen':
            collection_metadata['DateDatasetFrozen'] = val
            
        # Date --> Date
        elif key == 'Date':
            collection_metadata['Date'] = val
            
        # QAState --> QAState
        elif key == 'QAState':
            collection_metadata['QAState'] = val
            
        # DataDisclaimer --> DataDisclaimer
        elif key == 'DataDisclaimer':
            collection_metadata['DataDisclaimer'] = val
                        
    pp = pprint.PrettyPrinter(indent=4)
    pp.pprint(collection_metadata)
    pp.pprint(dataset_metadata)
    
    return { 
             'Collection':collection_metadata,
             'Dataset': dataset_metadata
             }
    
def write_metadata(metadata):
    '''
    Serializes metadata from Python dictionaries into a Python configuration file.
    '''
    
    collection_name = metadata['Collection']['CollectionName']
    collection_id = collection_name.replace(" ","_")
    config_file_dir = "%s/%s" % (labcas_metadata_dir, collection_id)
    if not os.path.exists(config_file_dir):
        os.makedirs(config_file_dir)
    config_file_name = collection_id + ".cfg"
    config_file_full_name = os.path.join(config_file_dir, config_file_name)

    with open(config_file_full_name, 'w') as f:
        
        # collection metadata
        f.write('[Collection]\n')
        for key, value in metadata['Collection'].items():
            f.write('%s=%s\n' % ( key, value))
            
        # dataset metadata
        f.write('[Dataset]\n')
        for key, value in metadata['Dataset'].items():
            f.write('%s=%s\n' % ( key, value))
            

if __name__== "__main__":
    
    # loop over directories
    filenames = os.listdir(ecas_metadata_dir)
    for filename in filenames: 
        if os.path.isdir(os.path.join(ecas_metadata_dir, filename)):
            
            # FIXME
            if filename == 'FHCRCHanashAnnexinLamr':
                input_xml_file = os.path.join(ecas_metadata_dir, ("%s.met" % filename))
                #output_cfg_dir = os.path.join(labcas_metadata_dir, filename)
                #if not os.path.exists(output_cfg_dir):
                #    os.makedirs(output_cfg_dir)
                #output_cfg_file =  os.path.join( output_cfg_dir, "%s.cfg" % filename )

                metadata = convert_metadata(input_xml_file)
                
                write_metadata(metadata)



