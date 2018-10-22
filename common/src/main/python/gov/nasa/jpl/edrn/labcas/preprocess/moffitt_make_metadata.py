# Python script to generate metadata for Moffitt datasets

import os
import sys
from glob import glob
from shutil import copyfile
import pydicom

# 1) 
# COLLECTION_NAME = "Sample_Mammography_Reference_Set"

# 2)
COLLECTION_NAME = "Automated_System_For_Breast_Cancer_Biomarker_Analysis"


DATA_DIR=os.environ['LABCAS_ARCHIVE'] + "/" + COLLECTION_NAME
METADATA_DIR=os.environ['LABCAS_METADATA'] + "/" + COLLECTION_NAME
INSTITUTION = "Moffitt"


def main():
        
    
    # loop over sub-directories == ddatasets
    subdirs = os.listdir(DATA_DIR)
    for subdir in subdirs:
        
        dataset_id = "%s/%s" % (COLLECTION_NAME, subdir)
        print("Processing sub-directory=%s, dataset_id=%s" % (subdir, dataset_id))
        
        # dataset directory
        dataset_dir = '%s/%s' % (DATA_DIR, subdir)
                        
        # create dataset metadata file
        template_file = METADATA_DIR + "/TEMPLATE_Moffitt.cfg"
        dataset_metadata_file =  dataset_dir + "/" + subdir + ".cfg"
    
        if not os.path.exists(dataset_metadata_file):
            
           print('Creating dataset metadata file: %s' % dataset_metadata_file)
    
           # read in template metadata file
           with open(template_file) as f:
              metadata = f.read()
    
           # replace metadata
           metadata = metadata.replace("subdir", subdir)
           if subdir[0]=='D':
               dataset_name = 'Dummy patient #%s (%s)' % (subdir[1:], INSTITUTION)
           elif subdir[0]=='C':
               dataset_name = 'Case #%s (unilateral breast cancer)' % subdir[1:]
           elif subdir[0]=='N':
               dataset_name = 'Case #%s (control)' % subdir[1:]
           else:
               dataset_name = 'Patient #%s (%s)' % (subdir[1:], INSTITUTION)
           dataset_description = dataset_name + " mammography images"
           metadata = metadata.replace("DATASET_ID", dataset_id)
           metadata = metadata.replace("DATASET_NAME", dataset_name)
           metadata = metadata.replace("DATASET_DESCRIPTION", dataset_description)
           # write out metadata
           with open(dataset_metadata_file, 'w') as f:
              f.write(metadata)
        
if __name__ == "__main__":
    main()
