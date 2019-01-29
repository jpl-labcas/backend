# Python script to migrate all ECAS data to LabCAS

import os
from xml.etree import ElementTree as ET
import pprint
import glob
import urllib.parse
from shutil import copyfile
import xml.sax.saxutils as saxutils
import re

# FIXME
# root_dir = "/Users/cinquini/data/ECAS_MIGRATION"
root_dir = "/home/cinquini/ECAS_MIGRATION"

ecas_metadata_dir = root_dir+"/datasets/"
ecas_data_dir = "/data/archive"
labcas_metadata_dir = root_dir+"/ecas-metadata/"
labcas_data_dir = root_dir+"/labcas_archive"

# RDF streams
sites_rdf_filepath = root_dir+"/rdf/sites.rdf"
leadpis_rdf_filepath = root_dir+"/rdf/registered-person.rdf"
organs_rdf_filepath = root_dir+"/rdf/body-systems.rdf"
protocols_rdf_filepath = root_dir+"/rdf/protocols.rdf"

# inverse maps are needed because sometimes the RDF element value is
# the object title, sometimes it's the object id...
sites_map = {}
inv_sites_map = {}
leadpis_map = {}
inv_leadpis_map = {}
organs_map = {}
inv_organs_map = {}
protocols_map = {}
inv_protocols_map = {}


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


def read_sites_from_rdf(rdf_filepath, metadata_map, inv_metadata_map):
    '''
    Parse an RDF file to populate metadata mappings
    '''

    namespaces = {'rdf':'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
                  'ns1':'http://edrn.nci.nih.gov/rdf/schema.rdf#',
                  'ns2':'http://purl.org/dc/terms/'}

    
    print("Reading %s" % rdf_filepath)
    
    xml = ET.parse(rdf_filepath)
    root_element = xml.getroot()
    
    # loop over tags:
    # <rdf:Description rdf:about="http://edrn.nci.nih.gov/data/sites/313">
    #     <ns2:title>CRUK Cambridge Research Institute</ns2:title>
    for description_element in root_element.findall('.//rdf:Description', namespaces):
        about_att = description_element.attrib.get('{%s}about' % namespaces['rdf'])
        rdf_id = about_att.split('/')[-1]
        for title_element in description_element.findall(".//ns2:title", namespaces):
            metadata_map[title_element.text] = rdf_id
            inv_metadata_map[rdf_id] = title_element.text


def read_organs_from_rdf(rdf_filepath, metadata_map, inv_metadata_map):
    '''
    Parses the RDF file containing organs information
    to map the organ title to the organ id.

    Example XML snippet:
    <rdf:Description rdf:about="http://edrn.nci.nih.gov/data/body-systems/32">
      <ns1:title>Thyroid</ns1:title>
      <rdf:type rdf:resource="http://edrn.nci.nih.gov/rdf/types.rdf#BodySystem"/>
    </rdf:Description>
    '''

    namespaces = {'rdf':'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
                  'ns1':'http://purl.org/dc/terms/'}


    print("Reading %s" % rdf_filepath)

    xml = ET.parse(rdf_filepath)
    root_element = xml.getroot()

    # loop over tags
    for description_element in root_element.findall('.//rdf:Description', namespaces):
        about_att = description_element.attrib.get('{%s}about' % namespaces['rdf'])
        rdf_id = about_att.split('/')[-1]
        title_element = description_element.find(".//ns1:title", namespaces)
        metadata_map[title_element.text] = rdf_id
        inv_metadata_map[rdf_id] = title_element.text


def read_leadpis_from_rdf(rdf_filepath, metadata_map, inv_metadata_map): 
    '''
    Reads LeadPIs information from the RDF file, maps last name + first name to id

    Example XML snippet:

    <rdf:Description rdf:about="http://edrn.nci.nih.gov/data/registered-person/1645">
      <ns2:surname>Lokshin</ns2:surname>
      <ns2:givenname>Anna</ns2:givenname>
    </rdf:Description>

    '''


    namespaces = {'rdf':'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
                  'ns1':'http://edrn.nci.nih.gov/rdf/schema.rdf#',
                  'ns2':'http://xmlns.com/foaf/0.1/',
                  'ns3':'http://www.w3.org/2001/vcard-rdf/3.0#'}


    print("Reading %s" % rdf_filepath)

    xml = ET.parse(rdf_filepath)
    root_element = xml.getroot()

    # loop over tags
    for description_element in root_element.findall('.//rdf:Description', namespaces):
        about_att = description_element.attrib.get('{%s}about' % namespaces['rdf'])
        rdf_id = about_att.split('/')[-1]
        lastname_element = description_element.find(".//ns2:surname", namespaces)
        firstname_element = description_element.find(".//ns2:givenname", namespaces)
        fullname = "%s %s" %(firstname_element.text, lastname_element.text)
        metadata_map["%s" % fullname]  = rdf_id
        inv_metadata_map[rdf_id] = fullname


def read_protocols_from_rdf(rdf_filepath, metadata_map, inv_metadata_map):
    '''
    Parses the RDF file containing protocol information
    to map the protocol title to the protocol id.

    Example XML snippet:
    <rdf:Description rdf:about="http://edrn.nci.nih.gov/data/protocols/site-specific/282-87">
        <ns2:title>Light Scattering Spectroscopy for the Detection of Colorectal Neoplasia</ns2:title>
    
    '''

    namespaces = {'rdf':'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
                  'ns1':'http://edrn.nci.nih.gov/rdf/schema.rdf#',
                  'ns2':'http://purl.org/dc/terms/'}

    print("Reading %s" % rdf_filepath)

    xml = ET.parse(rdf_filepath)
    root_element = xml.getroot()

    # loop over tags
    for description_element in root_element.findall('.//rdf:Description', namespaces):
        about_att = description_element.attrib.get('{%s}about' % namespaces['rdf'])
        rdf_id = about_att.split('/')[-1]
        for title_element in description_element.findall(".//ns2:title", namespaces):
           metadata_map[title_element.text] = rdf_id
           inv_metadata_map[rdf_id] = title_element.text

def addKeyValueToMap(map, key, value):
    '''
    Adds a value to a key, using "|" for multiple values.
    '''
    
    if key in map:
        map[key] = map[key] +"|" + value
    else:
        map[key] = value

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
    val = cleanup_text(description_element.text)
    collection_metadata['CollectionDescription'] = val
    
    metadata_element = root_element.find('.//metadata')
    for keyval_element in metadata_element.findall('./keyval'):
        key = keyval_element.find("key").text
        values = keyval_element.findall("val")
        for value in values:
            val = value.text
            
            # replace newline characters from metadata values
            if val:
                val = cleanup_text(val)
                # un-escape XML characters
                val = saxutils.unescape(val)
                     
            # DataSetName --> CollectionName, DatasetName
            if key == 'DataSetName':
                collection_metadata['CollectionName'] = val
                collection_metadata['CollectionId'] = val.replace(' ', '_')
                dataset_metadata['DatasetName'] = val
                dataset_metadata['DatasetDescription'] = val
                dataset_metadata['DatasetId'] = val.replace(" ","_")
                
            # ProtocolID --> ProtocolId, ProtocolName
            elif key == 'ProtocolID' or key == 'ProtocolId':
                addKeyValueToMap(collection_metadata, 'ProtocolId', val)
                
            elif key == 'ProtocolName':
                collection_metadata['ProtocolName'] = val
    
                if val in protocols_map:
                   collection_metadata['ProtocolId'] = protocols_map[val]
    
                # possibly override
                if val in inv_protocols_map:
                   collection_metadata['ProtocolId'] = val
                   collection_metadata['ProtocolName'] = inv_protocols_map[val]
                
            # LeadPI --> LeadPI, LeadPIId
            elif key == 'LeadPI':
                collection_metadata['LeadPI'] = val
    
                if leadpis_map.get(val, None):
                   collection_metadata['LeadPIId'] = leadpis_map[val]
                   
                # reverse mapping: PI id --> PI name
                elif inv_leadpis_map.get(val, None):
                    collection_metadata['LeadPIId'] = val
                    collection_metadata['LeadPI'] = inv_sites_map[val]
    
            # SiteName --> Institution, InstitutionId
            elif key == 'SiteName':
                collection_metadata['Institution'] = val
    
                if val in sites_map:
                   collection_metadata['InstitutionId'] = sites_map[val]
    
                # possibly override
                elif val in inv_sites_map:
                   collection_metadata['InstitutionId'] = val
                   collection_metadata['Institution'] = inv_sites_map[val]
    
            # DataCustodian --> DataCustodian
            elif key == 'DataCustodian':
                collection_metadata['DataCustodian'] = val
                
            # DataCustodianEmail --> DataCustodianEmail
            elif key == 'DataCustodianEmail':
                collection_metadata['DataCustodianEmail'] = val
                
            # OrganSite --> Organ, OrganId
            elif key == 'OrganSite':
                collection_metadata['Organ'] = val
    
                if val in organs_map:
                   collection_metadata['OrganId'] = organs_map[val]
    
                # possibly override
                elif val in inv_organs_map:
                   collection_metadata['OrganId'] = val
                   collection_metadata['Organ'] = inv_sites_map[val]
                
            # CollaborativeGroup --> CollaborativeGroup
            elif key == 'CollaborativeGroup':
                addKeyValueToMap(collection_metadata, 'CollaborativeGroup', val)
                
            # MethodDetails --> MethodDetails
            elif key == 'MethodDetails':
                collection_metadata['MethodDetails'] = val
                
            # ResultsAndConclusionSummary --> ResultsAndConclusionSummary
            elif key == 'ResultsAndConclusionSummary':
                collection_metadata['ResultsAndConclusionSummary'] = val
                
            # PubMedID --> PubMedID
            elif key == 'PubMedID':
                addKeyValueToMap(collection_metadata, 'PubMedID',
                                 "http://www.ncbi.nlm.nih.gov/pubmed/%s" % val)
                
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
                
            # ResearchSupport --> ResearchSupport
            elif key == 'ResearchSupport':
                addKeyValueToMap(collection_metadata, 'ResearchSupport', val)
                                
            # DatasetURL --> DatasetURL
            elif key == 'DatasetURL':
                addKeyValueToMap(collection_metadata, 'DatasetURL', val)
                
            # GrantSupport --> GrantSupport
            elif key == 'GrantSupport':
                addKeyValueToMap(collection_metadata, 'GrantSupport', val)
                
            # StudyBackground --> StudyBackground
            elif key == 'StudyBackground':
                addKeyValueToMap(collection_metadata, 'StudyBackground', val)
                
            # StudyMethods --> StudyMethods
            elif key == 'StudyMethods':
                addKeyValueToMap(collection_metadata, 'StudyMethods', val)
                
            # StudyResults --> StudyResults
            elif key == 'StudyResults':
                addKeyValueToMap(collection_metadata, 'StudyResults', val)
                
            # StudyConclusion --> StudyConclusion
            elif key == 'StudyConclusion':
                addKeyValueToMap(collection_metadata, 'StudyConclusion', val)
                
            
                        
    return { 
             'Collection':collection_metadata,
             'Dataset': dataset_metadata
             }
    
def write_product_type_metadata(metadata, collection_dir, dataset_dir):
    '''
    Serializes metadata from Python dictionaries into a Python configuration file.
    '''
    
    # collection metadata
    if not os.path.exists(collection_dir):
        os.makedirs(collection_dir)
        
    collection_id = metadata['Collection']['CollectionId']
    collection_config_file_name = collection_id + ".cfg"
    collection_config_file_path = os.path.join(collection_dir, collection_config_file_name)
    with open(collection_config_file_path, 'w') as f:
        
        f.write('[Collection]\n')
        for key, value in metadata['Collection'].items():
            f.write('%s=%s\n' % ( key, value))
            
    # dataset metadata
    if not os.path.exists(dataset_dir):
        os.makedirs(dataset_dir)
    dataset_id = metadata['Dataset']['DatasetId']
    dataset_config_file_name = dataset_id + ".cfg"
    dataset_config_file_path = os.path.join(dataset_dir, dataset_config_file_name)
    with open(dataset_config_file_path, 'w') as f:

        # dataset metadata
        f.write('[Dataset]\n')
        for key, value in metadata['Dataset'].items():
            # DatasetId is automatically computed from CollectionId + DatasetName
            if key != 'DatasetId':
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
            if val:
                val = cleanup_text(val)
                # reverse URL encoding
                val = urllib.parse.unquote(val).replace('+',' ')
                # un-escape XML characters
                val = saxutils.unescape(val)

                if key == 'CAS.ProductName' or key == 'CAS.ProductId' or key == 'CAS.ProductReceivedTime':
                    pass # ignore
                elif key == 'FileLocation':
                    pass # ignore since this is the old value and the new value will be added at publishing time
                else:
                    file_metadata[key] = val
            
        # add dictionary to list, if not empty
        if file_metadata:
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

def cleanup_text(val):
    '''
    Cleans up a text string.
    '''
    
    # remove new line characters
    val = val.replace('\n',' ').replace('\r\n',' ')
    # remove extra spaces
    val = re.sub("\s+"," ",val)
    # remove '\' character
    val = val.replace("\ ","")
    
    return val

if __name__== "__main__":
    
    # initialize metadata maps with information read from the RDF files
    read_sites_from_rdf(sites_rdf_filepath, sites_map, inv_sites_map)
    read_organs_from_rdf(organs_rdf_filepath, organs_map, inv_organs_map)
    read_leadpis_from_rdf(leadpis_rdf_filepath, leadpis_map, inv_leadpis_map)
    read_protocols_from_rdf(protocols_rdf_filepath, protocols_map, inv_protocols_map)
    
    # loop over directories
    filenames = os.listdir(ecas_metadata_dir)
    
    for filename in filenames: 
        dataset_dir = os.path.join(ecas_metadata_dir, filename)
        if os.path.isdir(dataset_dir):
            
            # FIXME
            # if filename == 'UPittLabMAPOvarianData':
            #if filename == 'Analysis_of_pancreatic_cancer_biomarkers_in_PLCO_set':
            #if filename == 'BCCA_Affy6.0RawData':
            #if filename == 'FHCRCHanashAnnexinLamr':
            if True:
            
                print("Processing: %s" % filename)
            
                input_xml_file = os.path.join(ecas_metadata_dir, ("%s.met" % filename))
 
                # read product type metadata from XML, convert to dictionaries
                metadata = read_product_type_metadata(input_xml_file)
                collection_id = metadata['Collection']['CollectionId']
                dataset_id = metadata['Dataset']['DatasetId']
                
                # write dictionary metadata to collection+dataset configuration file
                collection_dir = os.path.join(labcas_data_dir, collection_id)
                dataset_dir = os.path.join(collection_dir, dataset_id)
                write_product_type_metadata(metadata, collection_dir, dataset_dir)
                
                # read product type metadata from XML, convert to dictionaries
                file_metadata_array = read_product_metadata(dataset_dir)
                
                # copy data files
                output_dir = os.path.join(labcas_data_dir, collection_id, dataset_id)
                # FIXME
                #copy_products(collection_id, file_metadata_array, output_dir)
