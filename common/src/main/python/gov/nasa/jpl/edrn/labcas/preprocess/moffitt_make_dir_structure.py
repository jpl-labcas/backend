# Python script to pre-process MOFFITT data:
# o reorganizes the directory structire into LabCAS archive format
# o builds the file description by using the MOFIITT filename conventions
# o creates the dataset metadata file

import os
import sys
from glob import glob
from shutil import copyfile
import dicom
from utils import write_metadata

# process data from $LABCAS_ARCHIVE/Moffitt_BI --> $LABCAS_ARCHIVE/Sample_Mammography_Reference_Set
COLLECTION_NAME = "Sample_Mammography_Reference_Set"
TARGET_DATA_DIR=os.environ['LABCAS_ARCHIVE'] + "/" + COLLECTION_NAME
SRC_DATA_DIR=os.environ['LABCAS_ARCHIVE'] + "/Moffitt_BI"
METADATA_DIR=os.environ['LABCAS_METADATA'] + "/" + COLLECTION_NAME

        
def generate_file_description(filename):
    '''Generates the file description using project specific semantics.'''
    
    # create metadata file
    parts = filename.split("_")
    # patient #
    if (parts[0][0]=='D'):
        description = 'Dummy patient #%s' % parts[0][1:]
    else:
        description = 'Real patient #%s' % parts[0][1:]
    # packed/unpacked volume
    if parts[1]=='UV':
        description += ", unpacked volume frame # %s/%s" % (parts[2],parts[3])
    elif parts[1] == 'VOL':
        description += ", packed volume"
    # orientation
    for orientation in ['RCC','LCC','LMLO','RMLO']:
        if orientation in filename:
            description += ", orientation: %s" % orientation
            
    # image type
    if 'DAT' in filename:
        description += ", image type: raw"
    elif 'PRO' in filename:
        description += ", image type: processed"
    elif 'CV' in filename:
        description += ", image type: C-View"
    elif 'TRU' in filename:
        description += ", image type: Truth"

    return description

def main():
        
    # dataset directory
    #dataset = sys.argv[1]
    dataset_id = 'D0001'
    src_dataset_dir = '%s/%s' % (SRC_DATA_DIR, dataset_id)
    target_dataset_dir = '%s/%s' % (TARGET_DATA_DIR, dataset_id)
    
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
       print 'Creating dataset metadata file: %s' % dataset_metadata_file

       # read in template metadata file
       with open(template_file) as f:
          metadata = f.read()

       # replace metadata
       metadata = metadata.replace("DATASET_ID", dataset_id)
       if dataset_id[0]=='D':
           dataset_name = 'Dummy patient #%s' % dataset_id[1:]
       else:
           dataset_name = 'Real patient #%s' % dataset_id[1:]
       dataset_description = dataset_name + " mammography images"
       metadata = metadata.replace("DATASET_NAME", dataset_name)
       metadata = metadata.replace("DATASET_DESCRIPTION", dataset_description)
       # write out metadata
       with open(dataset_metadata_file, 'w') as f:
          f.write(metadata)
    
    
    # loop over DICOM files in dataset directory tree
    for root, dirs, files in os.walk(src_dataset_dir):
    
      for filename in files:
        f = "%s/%s" % (root, filename)
    
        # extract file metadata
        src_path = os.path.abspath(f)
       
        try:
           ds = dicom.read_file(f)
           tag_names = ds.dir()
           for tag_name in tag_names:
              data_element = ds.data_element(tag_name)
              if tag_name != 'PixelData':
                  print 'key=%s --> value=%s' % (tag_name, data_element.value)
           fid = ds.SOPInstanceUID
        
           # move and rename DICOM file
           # use DICOM identifier
           #dst_path = '%s/%s.dcm' % (target_version_dir, fid)
           # use original filename
           dst_path = '%s/%s.dcm' % (target_version_dir, filename)
           if not os.path.exists(dst_path):
              print '\nCopying DICOM file=%s --> %s' % (src_path, dst_path)
              copyfile(src_path, dst_path)
              
           metadata_filepath = dst_path + ".xmlmet"
           if not os.path.exists(metadata_filepath):
               description = generate_file_description(filename)
               write_metadata(metadata_filepath, description)
                           
        except Exception as e:
            print 'Error while processing file: %s' % src_path
            print e

if __name__ == "__main__":
    main()
