# Python script to pre-process MOFFITT data:
# o reorganizes the directory structure into LabCAS archive format
# o builds the file description by using the MOFIITT filename conventions
# o creates the dataset metadata file

import os
import sys
from glob import glob
from shutil import copyfile
import pydicom

# 1) 
COLLECTION_NAME = "Sample_Mammography_Reference_Set"
INSTITUTION = "Moffitt"


DATA_DIR=os.environ['LABCAS_ARCHIVE'] + "/" + COLLECTION_NAME
TEMPLATE_FILE = "%s/TEMPLATE_%s.cfg" % (DATA_DIR, INSTITUTION)

def get_top_dataset_name(subDir):
    
    patientNumber = subDir[1:5]
    datasetName = "Patient #%s (%s)" % (patientNumber, INSTITUTION)
    print("\tDataset name=%s" % datasetName)
    return datasetName
    
def read_metadata_from_template(datasetName):
    
    # read in template metadata file
    with open(TEMPLATE_FILE) as f:
        metadata = f.read()
                
    # replace metadata
    datasetDescription = datasetName + " mammography images"
    metadata = metadata.replace("DATASET_NAME", datasetName)
    metadata = metadata.replace("DATASET_DESCRIPTION", datasetDescription)

    return metadata

def write_metadata(dirPath, metadata):
    
    (parentDir, thisDir) = os.path.split(dirPath)
    datasetMetadataFile = dirPath + "/" + thisDir + ".cfg"
    print("\tWriting out metadata to file: %s" % datasetMetadataFile)
    
    with open(datasetMetadataFile, 'w') as f:
        f.write(metadata)
    
def get_sub_dataset_name(subDirPath):
    
    (parentDir, thisDir) = os.path.split(subDirPath)
    
    if thisDir.lower() == 'primary':
        subDatasetName = 'Primary Images'
    elif thisDir.lower() == 'mammograms':
        subDatasetName = 'Mammograms'
    elif thisDir.lower() == 'truth':
        subDatasetName = 'Truth files'
    elif thisDir.lower() == '2d':
        subDatasetName = 'Full-Field Digital Mammography (FFDM) images (2D)'
    elif thisDir.lower() == 'volume':
        subDatasetName = 'Digital Breast Tomosynthesis  (DBT) images (3D)'
    elif thisDir.lower() == 'proc':
        subDatasetName = 'Processed (for display)'
    elif thisDir.lower() == 'raw':
        subDatasetName = 'Raw (for processing)'
    elif thisDir.lower() == 'cview':
        subDatasetName = 'C-View'
    else:
        subDatasetName = subDirPath
        
    print("\tSub-dataset name=%s" % subDatasetName)
    return subDatasetName

def get_file_description(fileName):
    
    return fileName

def main():
    
    # loop over 1st level directories
    subDirs = os.listdir(DATA_DIR)
    for subDir in subDirs:
        
        subDirPath = os.path.join(DATA_DIR, subDir)
        if os.path.isdir(subDirPath):
            print("Processing directory: %s" % subDir)
            
            # read and parse metadata from template file
            datasetName = get_top_dataset_name(subDir)
            metadata = read_metadata_from_template(datasetName)
            
            # write out the dataset metadata to file
            write_metadata(subDirPath, metadata)
            
        # traverse all sub-directories
        for dirName, subDirList, fileList in os.walk(subDirPath):
            
            # skip the first directory
            if dirName != subDirPath:
                print('\tProcessing sub-directory: %s' % dirName)
                subDatasetName = get_sub_dataset_name(dirName)
                
                # write out the sub-dataset metadata to file
                metadata = "[Dataset]"
                metadata += "\nDatasetName=%s" % subDatasetName
                metadata += "\n"
                write_metadata(dirName, metadata)
                
                # loop over files in directory
                for fileName in fileList:
                    if fileName.endswith(".dcm"):
                        filePath = os.path.join(dirName, fileName)
                        print("\t\tProcessing DICOM file: %s" % filePath)
                        
                        fileDescription = get_file_description(fileName)
                        
                        # extract metadata from DICOM header
                        try:
                           ds = pydicom.read_file(f)
                           tag_names = ds.dir()
                           for tag_name in tag_names:
                              data_element = ds.data_element(tag_name)
                              if tag_name != 'PixelData' and data_element and data_element.value:
                                  print('key=%s --> value=%s' % (tag_name, data_element.value))
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

            
            
    
    '''
    # traverse the directory tree untill files are found
    for dirName, subdirList, fileList in os.walk(DATA_DIR):
        print('Processing directory: %s' % dirName)
        
        if len(fileList) > 0:
                        
            # create dataset metadata file
            (parentDir, thisDir) = os.path.split(dirName)
            dataset_metadata_file = dirName + "/" + thisDir + ".cfg"
            #if not os.path.exists(dataset_metadata_file):
            print('Creating dataset metadata file: %s' % dataset_metadata_file)
            
            # read in template metadata file
            with open(TEMPLATE_FILE) as f:
                metadata = f.read()
                
            # replace metadata
            dataset_name = get_dataset_name(thisDir)
            dataset_description = dataset_name + " mammography images"
            metadata = metadata.replace("DATASET_NAME", dataset_name)
            metadata = metadata.replace("DATASET_DESCRIPTION", dataset_description)
            # write out metadata
            with open(dataset_metadata_file, 'w') as f:
               f.write(metadata)

        # process all files
        #for fname in fileList:
        #    fpath = os.path.join(dirName, fname)
        #    print('\tProcessing file: %s' % fpath)
    '''
        
    '''
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
               dataset_name = 'Dummy patient # %s (%s)' % (dataset_id[1:], INSTITUTION)
           elif dataset_id[0]=='C':
               dataset_name = 'Case # %s (unilateral breast cancer)' % dataset_id[1:]
           elif dataset_id[0]=='N':
               dataset_name = 'Case # %s (control)' % dataset_id[1:]
           else:
               dataset_name = 'Patient # %s (%s)' % (dataset_id[1:], INSTITUTION)
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
            print(src_path)      
            try:
               ds = pydicom.read_file(f)
               tag_names = ds.dir()
               for tag_name in tag_names:
                  data_element = ds.data_element(tag_name)
                  if tag_name != 'PixelData' and data_element and data_element.value:
                      print('key=%s --> value=%s' % (tag_name, data_element.value))
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
    '''

if __name__ == "__main__":
    main()
