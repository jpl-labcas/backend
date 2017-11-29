# Python script to generate dataset metadata for the MDAnderson IPMN data

import os
import glob
import shutil

# parameters
COLLECTION_NAME = 'MD_Anderson_Pancreas_IPMN_images'

ARCHIVE_DIR="/labcas-data/MD_Anderson_Pancreas_IPMN_images"

# loop over sub-directories
for isubdir in os.listdir(ARCHIVE_DIR):
    
    # replace blanks
    dataset_id = isubdir.replace(' ','_')
    parts = dataset_id.split('-')
    dataset_name = "%s Panel %s Case %s" % (parts[0], parts[1][1:], parts[2])
    dataset_desc = dataset_name
    print "Processing subdir: %s --> %s" % (isubdir, dataset_name)
    
    # create dataset metadata file
    template_file = os.environ['LABCAS_METADATA'] + "/" + COLLECTION_NAME + "/TEMPLATE.cfg"
    dataset_metadata_file = ARCHIVE_DIR + "/" + dataset_id + "/" + dataset_id + ".cfg"

    if not os.path.exists(dataset_metadata_file):
       print 'Creating dataset metadata file: %s' % dataset_metadata_file

       # read in template metadata file
       with open(template_file) as f:
          metadata = f.read()

       # replace metadata
       metadata = metadata.replace("DATASET_ID", dataset_id)
       metadata = metadata.replace("DATASET_NAME", dataset_name)
       metadata = metadata.replace("DATASET_DESCRIPTION", dataset_desc)
    
       # write out metadata
       with open(dataset_metadata_file, 'w') as f:
          f.write(metadata)
