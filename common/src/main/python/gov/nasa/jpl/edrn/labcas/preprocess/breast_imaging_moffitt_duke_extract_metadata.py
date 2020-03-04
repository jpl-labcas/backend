# Python script to pre-process MOFFITT data:
# o reorganizes the directory structure into LabCAS archive format
# o builds the file description by using the MOFIITT filename conventions
# o creates the dataset metadata file

import os
import sys
from glob import glob
from shutil import copyfile
import pydicom
from gov.nasa.jpl.edrn.labcas.server.dicom_met_extractor import get_top_dataset_name

# 1) 
COLLECTION_NAME = "Combined_Imaging_and_Blood_Biomarkers_for_Breast_Cancer_Diagnosis"
# INSTITUTION = "Moffitt"
INSTITUTION = "Duke"

# 2)
# COLLECTION_NAME = "Automated_System_For_Breast_Cancer_Biomarker_Analysis"
# INSTITUTION = "Moffitt"

# 3)
#COLLECTION_NAME='Automated_Quantitative_Measures_of_Breast_Density_Data'
#INSTITUTION = "Moffitt"

DATA_DIR=os.environ['LABCAS_ARCHIVE'] + "/" + COLLECTION_NAME
TEMPLATE_FILE = "%s/TEMPLATE_%s.cfg" % (DATA_DIR, INSTITUTION)
    
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
    elif thisDir.lower() == 'ffdm':
        subDatasetName = 'Full-Field Digital Mammography (FFDM) images (2D)'
    elif thisDir.lower() == 'volume':
        subDatasetName = 'Digital Breast Tomosynthesis  (DBT) images (3D)'
    elif thisDir.lower() == 'proc':
        subDatasetName = 'Processed (for display)'
    elif thisDir.lower() == 'raw':
        subDatasetName = 'Raw (for processing)'
    elif thisDir.lower() == 'cview':
        subDatasetName = 'C-View'
    elif thisDir.lower() == 'mask':
        subDatasetName = 'Mask'
    else:
        subDatasetName = subDirPath
        
    print("\tSub-dataset name=%s" % subDatasetName)
    return subDatasetName

def main():
    
    # loop over 1st level directories
    subDirs = os.listdir(DATA_DIR)
    for subDir in subDirs:
        
        subDirPath = os.path.join(DATA_DIR, subDir)
        if os.path.isdir(subDirPath):
            print("Processing directory: %s" % subDir)
            
            # read and parse metadata from template file
            datasetName = get_top_dataset_name(subDir[0], subDir[1:5])
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
                

if __name__ == "__main__":
    main()
