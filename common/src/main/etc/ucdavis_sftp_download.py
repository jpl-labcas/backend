# Python script that downloads data from the UCDavis SFTP server
# SFTP credentials are retrieved from the environment.
# The script will process all directories under a given root,
# and skip files that have already been downloaded.

# Usage examples: 
# - python ucdavis_sftp_download.py /labcas-data/UCDavis/
# - python ucdavis_sftp_download.py /labcas-data/UCDavis/MMHCC_Image_Archive/
# - python ucdavis_sftp_download.py /labcas-data/UCDavis/MMHCC_Image_Archive/Human_Breast/

import sys
import csv
import os
import pysftp
import re
from glob import glob
from xml.sax.saxutils import escape

HOST = "sftp.mousebiology.org"
USERNAME = os.environ['SFTP_USERNAME']
PASSWORD = os.environ['SFTP_PASSWORD']

# dictionary of file-level metadata fields
FILE_METADATA = {'Study Name/Number':'labcas.pathology:Study',
                 'Experiment Name/Number':'labcas.pathology:Experiment',
                 'Accession Number':'labcas.pathology:AccessionNumber',
                 'Data Group':'labcas.pathology:DataGroup',
                 'Cohort':'labcas.pathology:Cohort',
                 'Sub-Cohort':'labcas.pathology:Sub-Cohort',
                 'Cohort Experimental Description':'labcas.pathology:CohortExperimentalDescription',
                 'Diagnosis':'labcas.pathology:Diagnosis',
                 'NCIT DIAGNOSIS CODE':'labcas.pathology:NCITDiagnosisCode',
                 'NCIT ORGAN CODE':'labcas.pathology:NCITOrganCode',
                 'NCIT PROCEDURE':'labcas.pathology:NCITProcedure',
                 'Microscopic Description':'labcas.pathology:MicroscopicDescription',
                 'Gross Description':'labcas.pathology:GrossDescription',
                 'Specimen ID':'labcas.pathology:SpecimenId',
                 'Animal Type':'labcas.pathology:AnimalType',
                 'Organ System':'labcas.pathology:OrganSystem',
                 'Organ Site':'labcas.pathology:OrganSite',
                 'Gender':'labcas.pathology:Gender',
                 'Parity':'labcas.pathology:Parity',
                 'Fixative':'labcas.pathology:Fixative',
                 'Description (Accession #)':'labcas.pathology:AccessionNumberDescription',
                 'Stain':'labcas.pathology:Stain',
                 'Quality Factor':'labcas.pathology:QualityFactor',
                 'Captured Date':'labcas.pathology:CaptureDate',
                 'Scan Status':'labcas.pathology:ScanStatus',
                 'Image ID':'labcas.pathology:ImageId' }

# maps for NCIT codes
ncit_diagnosis_code = { 'C21667':'MIN',
                        'C21679':'CANCER',
                        'C29583':'EMT',
                        'C21696':'OTHER',
                        'C21662':'BENIGN NEOPLASM',
                        'C3577':'MET IN LUNG',
                        'C21652':'HYPERPLASIA_NOS' }

ncit_organ_code = { 'C22549':'MuMAMMARY GLAND',
                    'C12367':'MAMMARY GLAND',
                    'C22600':'MuLUNG',
                    'C12468':'LUNG',
                    'C22671':'MuUTERUS',
                    'C12405':'UTERUS',
                    'C22515':'MuLIVER',
                    'C12392':'LIVER' }

ncit_procedure_code = { 'C15277':'MASTECTOMY',
                        'C15755':'LUMPECTTOMY',
                        'C15342':'TRANSPLANT',
                        'C22490':'TUMOR CELL TRANSPLANT',
                        'C68623':'NECROPSY',
                        'C16490':'CYTOLOGY',
                        'C15189':'BIOPSY',
                        'C51698':'BIOPSY OF BREAST',
                        'C15680':'CORE BIOPSY' }

                 
# function to download all files in a dataset from the SFTP server
def download_dataset(sftp_server, csv_file_path):
        
    # get the directory and file listing
    #remote_listing = sftp_server.listdir()
    # prints out the directories and files, line by line
    #for i in remote_listing:
    #    print i
    
    # keep track of files
    files = []
    
    print '\nReading CSV file: %s' % csv_file_path
    # output directory (must include version)
    target_dir = os.path.split(csv_file_path)[0] + "/1"
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
    print 'Downloading files to directory: %s' % target_dir    
    
    # parse CSV file, extract the 'File Location' field
    with open(csv_file_path) as csvfile:
        
        reader = csv.DictReader(csvfile)
        
        for row in reader:
            try:
                src_file_path=row['File Location']
                print 'Detected src_file_path: %s' % src_file_path
                
                # 1) Download file from sftp_path to target_file_path
                # examples of 'File Location':
                # \\ap1314-dsr\Images2\MMHCC Image Archive\Human Breast\MC02-0720.sid.svs
                # \\ap1314-dsr\Images3\MC04\MC04-0006.sid.svs
                if 'images' in src_file_path.lower() and src_file_path.lower().endswith('svs'):
                    print 'Attempting to download SVS file: %s' % src_file_path
                    #truncated_file_path = re.sub('.*(?i)images\d?','', src_file_path)
                    parts = src_file_path.split("\\")
                    sftp_path = "/".join(parts[3:]) # remove '\\ap1314-dsr'
                    sftp_path = sftp_path.replace('images', 'Images')
                    target_file_path = "%s/%s" % (target_dir, parts[-1])
                    print 'Matching to target_file_path: %s' % target_file_path
                    
                    if target_file_path in files:
                        print 'Target file already found: %s' % target_file_path
                    else:
                        files.append(target_file_path)
                        
                    if not os.path.exists(target_file_path) or os.path.getsize(target_file_path) == 0:
                        print "\tDownloading: %s to: %s" % (sftp_path, target_file_path)
                        try:
                            sftp_server.get(sftp_path, target_file_path)
                        except Exception as e:
                            print 'Error downloading: %s' % e
                        # cleanup from crashed downloads so bad files don't get published
                        if os.path.exists(target_file_path) and os.path.getsize(target_file_path) == 0:
                            os.remove(target_file_path)
                    else:
                        print 'File %s : %s already exists, skipping' % (sftp_path, target_file_path)
                        
                    #2) Extract file metadata from CSV file to target_file_path.xmlmet
                    if os.path.exists(target_file_path):
                        extract_file_metadata(row, target_file_path +".xmlmet")
            
            except KeyError as e:
                print 'WARNING: File Location not found for row: %s' % row

# function to serialize CSV file metadata to XML
def extract_file_metadata(metadata, met_filepath):
    
    # extract metadata, write output file
    with open(met_filepath,'w') as file: 
        file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
        for cvs_key, solr_key in FILE_METADATA.items():
            if cvs_key in metadata and metadata.get(cvs_key, None) : # not null field value found in CSV row
              value = str(metadata[cvs_key])
              # map NCIT code to its value, or default to the code
              if cvs_key == 'NCIT DIAGNOSIS CODE':
                value  = ncit_diagnosis_code.get(value, default=value)
              elif cvs_key == 'NCIT ORGAN CODE':
                value  = ncit_organ_code.get(value, default=value)
              elif cvs_key == 'NCIT PROCEDURE':
                value  = ncit_procedure_code.get(value, default=value)
              
              #print 'key=%s --> value=%s' % (cvs_key, metadata[cvs_key])
              file.write('\t<keyval type="vector">\n')
              file.write('\t\t<key>_File_%s</key>\n' % str(solr_key))
              file.write('\t\t<val>%s</val>\n' % escape( value ) ) # NOTE: must escape XML entities such as & <> "
              file.write('\t</keyval>\n')
             
        # write additional file 'description' field
        desc_value = ''
        if metadata.get('Accession Number', None):
            desc_value = "Accession Number=%s" % metadata['Accession Number']
        if metadata.get('Stain', None):
            desc_value += ", stain=%s" % metadata['Stain']
        write_file_description(file, escape(str(desc_value)) ) # NOTE: must escape XML entities such as & <> "
                
        file.write('</cas:metadata>\n')

def write_file_description(file, desc_value):
    
    file.write('\t<keyval type="vector">\n')
    file.write('\t\t<key>_File_Description</key>\n')
    file.write('\t\t<val>%s</val>\n' % escape( str(desc_value) ) ) # NOTE: must escape XML entities such as & <> "
    file.write('\t</keyval>\n')


# main script
if __name__ == "__main__":
    
    # open connection to the SFTP server
    # NOTE that the SFTP server only allows less than 20 connections in 4 minutes
    # so the connection must be re-used across several datasets
    sftp_server = pysftp.Connection(host=HOST, username=USERNAME, password=PASSWORD)

    # loop over all CSV files under root directory
    root_target_dir = sys.argv[1]
    csv_file_paths = [y for x in os.walk(root_target_dir) for y in glob(os.path.join(x[0], '*.csv'))]
    for csv_file_path in csv_file_paths:    
        download_dataset(sftp_server, csv_file_path) 
        
    # close the SFTP connection
    sftp_server.close()

        
