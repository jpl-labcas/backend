# Python script to migrate all ECAS data to LabCAS

import os
from xml.etree import ElementTree as ET
import pprint
import glob
import urllib.parse
from shutil import copyfile

ecas_metadata_dir = "/home/cinquini/ECAS_MIGRATION/datasets/"
ecas_data_dir = "/data/archive"
labcas_metadata_dir = "/home/cinquini/ECAS_MIGRATION/ecas-metadata/"
labcas_data_dir = "/home/cinquini/ECAS_MIGRATION/labcas_archive"

pp = pprint.PrettyPrinter(indent=4)

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

def read_product_type_metadata(input_xml_file):
    '''
    Reads product type metadata from XML and converts it to Python dictionaries.
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
    for keyval_element in metadata_element.findall('./keyval'):
        key = keyval_element.find("key").text
        val = keyval_element.find("val").text
        # DataSetName --> CollectionName, DatasetName
        if key == 'DataSetName':
            collection_metadata['CollectionName'] = val
            collection_metadata['CollectionId'] = val.replace(' ', '_')
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
                        
    #pp.pprint(collection_metadata)
    #pp.pprint(dataset_metadata)
    
    return { 
             'Collection':collection_metadata,
             'Dataset': dataset_metadata
             }
    
def write_product_type_metadata(metadata):
    '''
    Serializes metadata from Python dictionaries into a Python configuration file.
    '''
    
    collection_id = metadata['Collection']['CollectionId']
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
            
def read_product_metadata(dataset_dir):
    
    # list metadata files in all sub-direcories
    met_files = glob.glob('%s/**/*.met' % dataset_dir, recursive=True)
    
    file_metadata_array = []
    
    for met_file in met_files:
        
        file_metadata = {}
        
        xml = ET.parse(met_file)
        root_element = xml.getroot()
        
        for keyval_element in root_element.findall('./keyval'):
            key = keyval_element.find("key").text
            val = keyval_element.find("val").text  
            val = urllib.parse.unquote(val).replace('+',' ')
            
            if key == 'CAS.ProductName' or key == 'CAS.ProductId' or key == 'CAS.ProductReceivedTime':
                pass # ignore
            else:
                file_metadata[key] = val
            
        #pp.pprint(file_metadata)
        
        file_metadata_array.append(file_metadata)
        
    return file_metadata_array
        
def copy_products(collection_id, file_metadata_array, output_dir):
    
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    for file_metadata in file_metadata_array:
        
        filename = file_metadata['Filename']
        output_file = os.path.join(output_dir, filename)
        
        input_file = find_most_recent_file(ecas_data_dir, filename)
        if input_file:
            
            # copy file
            print("Copying file: %s --> %s" % (input_file, output_file))
            copyfile(input_file, output_file)
            
            # write associated metadata
            write_product_metadata(file_metadata, output_file)
            
        else:
            print("Cannot find file: '%s' [collection: %s]" % (filename, collection_id) )

            
def write_product_metadata(file_metadata, output_file):
    
    met_file = output_file + ".xmlmet"
    with open(met_file, 'w') as f:
        f.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
        
        # FIXME: select specific elements
        for key, value in file_metadata.items():
            f.write('\t<keyval type="vector">\n')
            f.write('\t\t<key>_File_%s</key>\n' % key)
            f.write('\t\t<val>%s</val>\n' % value)
            f.write('\t</keyval>\n')
        f.write('</cas:metadata>')
        
def find_most_recent_file(root_dir, file_name):
    result = None
    mtime = None
    for root, dirs, files in os.walk(root_dir):
        if file_name in files:
            file_full_path = os.path.join(root, file_name)
            file_mod_time = os.path.getmtime(file_full_path)
            if mtime:
                if file_mod_time > mtime:
                    result = file_full_path
                    mtime = file_mod_time
            else:
                result = file_full_path
                mtime = file_mod_time
    return result

if __name__== "__main__":
    
    # loop over directories
    filenames = os.listdir(ecas_metadata_dir)
    
    for filename in filenames: 
        dataset_dir = os.path.join(ecas_metadata_dir, filename)
        if os.path.isdir(dataset_dir):
            
            # FIXME
            #if filename == 'FHCRCHanashAnnexinLamr':
            #if filename == 'WHIColonWistarSpeicher':
            if True:
                input_xml_file = os.path.join(ecas_metadata_dir, ("%s.met" % filename))
 
                # read product type metadata from XML, convert to dictionaries
                metadata = read_product_type_metadata(input_xml_file)
                collection_id = metadata['Collection']['CollectionId']
                dataset_id = metadata['Dataset']['DatasetId']
                
                # write dictionary metadata to collection+dataset configuration file
                write_product_type_metadata(metadata)
                
                # read product type metadata from XML, convert to dictionaries
                file_metadata_array = read_product_metadata(dataset_dir)
                
                # copy data files
                # FIXME: remove 1
                output_dir = os.path.join(labcas_data_dir, collection_id, dataset_id, '1')
                copy_products(collection_id, file_metadata_array, output_dir)



