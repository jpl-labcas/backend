# Python script to generate dataset metadata for the MDAnderson IPMN data

import os
import glob
import shutil
import csv

# parameters
COLLECTION_NAME = 'MD_Anderson_Pancreas_IPMN_images'
ARCHIVE_DIR="/labcas-data/MD_Anderson_Pancreas_IPMN_images"
#ARCHIVE_DIR="/usr/local/labcas_archive/MD_Anderson_Pancreas_IPMN_images"
SPREADSHEET = 'IPMN Project Clinicopathological Information MDACC 12052017.csv'

# read data from spreadsheet
csv_file_path = os.environ['LABCAS_METADATA'] + "/" + COLLECTION_NAME + "/" + SPREADSHEET
csvdata = {}
with open(csv_file_path) as csvfile:
    print 'Importing data from CSV spreadsheet: %s' % csv_file_path
    reader = csv.DictReader(csvfile)
    for row in reader:
      try:
        # change IPMN-01 --> IPMN-001
        key = row['Case#'].replace('IPMN-','IPMN-0')
        csvdata[ key ] = row
      except Exception as e:
        print e


# loop over sub-directories
for isubdir in os.listdir(ARCHIVE_DIR):
    
    # skip IPMN-Ancillary-Data
    if 'IPMN-0' in isubdir:
    
        dataset_id = isubdir
        parts = dataset_id.split('-')
        dataset_name = "IPMN Case %s" % parts[1]
        dataset_desc = dataset_name
        print "Processing subdir: %s --> %s" % (isubdir, dataset_name)
    
        # create dataset metadata file
        template_file = os.environ['LABCAS_METADATA'] + "/" + COLLECTION_NAME + "/TEMPLATE.cfg"
        dataset_metadata_file = ARCHIVE_DIR + "/" + dataset_id + "/" + dataset_id + ".cfg"

        print 'Creating dataset metadata file: %s' % dataset_metadata_file

        # read in template metadata file
        with open(template_file) as f:
            metadata = f.read()

            # replace metadata
            metadata = metadata.replace("DATASET_ID", dataset_id)
            metadata = metadata.replace("DATASET_NAME", dataset_name)
            metadata = metadata.replace("DATASET_DESCRIPTION", dataset_desc)
            metadata = metadata.replace("AGE", csvdata[dataset_id]['Age'])
            metadata = metadata.replace("GENDER", csvdata[dataset_id]['Gender'])
            metadata = metadata.replace("RACE", csvdata[dataset_id]['Race'])
            metadata = metadata.replace("DIAGNOSIS", csvdata[dataset_id]['Diagnosis'])
            metadata = metadata.replace("COMMENTS", csvdata[dataset_id]['Comments'])
            metadata = metadata.replace("SIZE_OF_CYST", csvdata[dataset_id]['Size of Cyst'])
            
            # write out metadata
            with open(dataset_metadata_file, 'w') as f:
                f.write(metadata)