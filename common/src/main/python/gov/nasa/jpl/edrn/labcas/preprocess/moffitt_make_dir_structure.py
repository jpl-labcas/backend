# Python script to pre-process MOFFITT data:
# o reorganizes the directory structure into LabCAS archive format
# o builds the file description by using the MOFIITT filename conventions
# o creates the dataset metadata file

import os
import sys
from glob import glob
from shutil import copyfile
import pydicom

# process data from $LABCAS_ARCHIVE/Moffitt_BI --> $LABCAS_ARCHIVE/Sample_Mammography_Reference_Set
COLLECTION_NAME = "Sample_Mammography_Reference_Set"
TARGET_DATA_DIR=os.environ['LABCAS_ARCHIVE'] + "/" + COLLECTION_NAME
SRC_DATA_DIR=os.environ['LABCAS_ARCHIVE'] + "/Moffitt_BI/Test1_447_20180921"
METADATA_DIR=os.environ['LABCAS_METADATA'] + "/" + COLLECTION_NAME
INSTITUTION = "Moffitt"

def main():
        
    # dataset directory
    #dataset = sys.argv[1]
    #dataset_id = 'E0010'
    for dataset_id in os.listdir(SRC_DATA_DIR):
        src_dataset_dir = '%s/%s' % (SRC_DATA_DIR, dataset_id)
        target_dataset_dir = '%s/%s' % (TARGET_DATA_DIR, dataset_id)
    
    # loop over sub-directories == ddatasets
    subdirs = os.listdir(SRC_DATA_DIR)
    for dataset_id in subdirs:
        
        print("Processing sub-directory: %s" % dataset_id)
        
        # dataset directory
        #dataset = sys.argv[1]
        #dataset_id = 'C0001'
        src_dataset_dir = '%s/%s' % (SRC_DATA_DIR, dataset_id)
        target_dataset_dir = '%s/%s' % (TARGET_DATA_DIR, dataset_id)
        
>>>>>>> master
        # dataset version directory
        target_version_dir = '%s/1' % target_dataset_dir
        if not os.path.exists(target_version_dir):
            os.makedirs(target_version_dir)
                
        # create dataset metadata file
        template_file = METADATA_DIR + "/TEMPLATE_Moffitt.cfg"
        dataset_archive_dir = TARGET_DATA_DIR + "/" + dataset_id
        if not os.path.exists(dataset_archive_dir):
            os.makedirs(dataset_archive_dir)
        dataset_metadata_file =  dataset_archive_dir + "/" + dataset_id + ".cfg"
    
        if not os.path.exists(dataset_metadata_file):
            
           print('Creating dataset metadata file: %s' % dataset_metadata_file)
    
           # read in template metadata file
           with open(template_file) as f:
              metadata = f.read()
    
           # replace metadata
           metadata = metadata.replace("DATASET_ID", dataset_id)
           if dataset_id[0]=='D':
               dataset_name = 'Dummy patient #%s (%s)' % (dataset_id[1:], INSTITUTION)
           else:
               dataset_name = 'Patient #%s (%s)' % (dataset_id[1:], INSTITUTION)
           dataset_description = dataset_name + " mammography images"
           metadata = metadata.replace("DATASET_NAME", dataset_name)
           metadata = metadata.replace("DATASET_DESCRIPTION", dataset_description)
           # write out metadata
           with open(dataset_metadata_file, 'w') as f:
              f.write(metadata)
        
<<<<<<< HEAD
        
        # loop over DICOM files in dataset directory tree
        for root, dirs, files in os.walk(src_dataset_dir):
        
=======
        
        # loop over DICOM files in dataset directory tree
        for root, dirs, files in os.walk(src_dataset_dir):
        
>>>>>>> master
          for filename in files:
            f = "%s/%s" % (root, filename)
        
            # extract file metadata
            src_path = os.path.abspath(f)
<<<<<<< HEAD
            print(src_path)      
=======
           
>>>>>>> master
            try:
               ds = pydicom.read_file(f)
               tag_names = ds.dir()
               for tag_name in tag_names:
                  data_element = ds.data_element(tag_name)
<<<<<<< HEAD
                  if tag_name != 'PixelData' and data_element and data_element.value:
                      print('key=%s --> value=%s' % (tag_name, data_element.value))
=======
                  #if tag_name != 'PixelData' and data_element and data_element.value:
                  #    print('key=%s --> value=%s' % (tag_name, data_element.value))
>>>>>>> master
               fid = ds.SOPInstanceUID
            
               # move and rename DICOM file
               # use DICOM identifier
               #dst_path = '%s/%s.dcm' % (target_version_dir, fid)
               # use original filename
               dst_path = '%s/%s' % (target_version_dir, filename)
               if not os.path.exists(dst_path):
                  print('\nCopying DICOM file=%s --> %s' % (src_path, dst_path))
                  copyfile(src_path, dst_path)
                                             
            except Exception as e:
                print('Error while processing file: %s' % src_path)
                print(e)

if __name__ == "__main__":
    main()
