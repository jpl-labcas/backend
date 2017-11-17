# script to organize the University of Miami DICOM data

from os import listdir, renames, mkdir
from os.path import isdir, join, exists
import re
import glob
import shutil

# Mac collection root directory:
#collection_dir = '/usr/local/labcas_staging/MAST'
# LabCAS root directory:
collection_dir = '/labcas-data/MAST'

def make_dataset_id(dataset_name):
    return re.sub('[(){}\[\]]+', '', dataset_name)

def make_dataset_description(patient_id, dataset_name):
    
    description ="Patient %s " % patient_id
    
    if 'WO_Apparent.Diffusion.Coefficient' in dataset_name:
        description += "Apparent Diffusion Coefficient (ADC) data"
    elif 'WO_DualEcho' in dataset_name:
        description += "DualEcho (DE) data"
    elif 'WO_DIFF' in dataset_name:
        description += "Diffusion Weighted Imaging (DWI) data"
    elif 'WO_LAVA' in dataset_name:
        description += "Dynamic Contrast Enhancing (DCE) data"
    elif 'WO_Resliced.Derived' in dataset_name:
        description += "T2 data"
    elif 'WO_T2.AX.SFOV' in dataset_name:
        description += "T2 data restricted to a small field of view (SFOV), from the axial (top-down) perspective"
    elif 'WO_T2.COR.SFOV' in dataset_name:
        description += "T2 data restricted to a small field of view (SFOV), from the coronal (back-to-front) perspective"
    elif 'WO_T2.FSE.AX' in dataset_name:
        description += "T2 data"
    elif 'WO_T2.FSE.FS.AX' in dataset_name:
        description += "Fat-saturated T2 data, where fat and vascular tissue appear bright white"
    elif 'WO_T2.SAG.SFOV' in dataset_name:
        description += "T2 data in the small field of view (SFOV), from the sagittal (left-to-right) perspective"
    elif 'WO_HRS.BL_n1_' in dataset_name:
        description += "RTDICOM structure associated with the eighth series. It has structures which we created using our custom software and manual."
    elif 'WO_M041.BL_n1' in dataset_name:
        description += "RTDICOM structure associated with the fifth series. It is the biopsy needle tracks for this patient."
    else:
        description = dataset_name
        
    return description

def make_dataset_metadata_file(collection_dir, dataset_name, dataset_id):
    
    filename = join(collection_dir, dataset_id, dataset_id+".cfg")
    
    if not exists(filename):
        print 'Writing dataset metadata to: %s' % filename
        
        parts = dataset_name.split("_")
        with open(filename,"w") as file: 
            lines = ["[Dataset]\n",
                     "DatasetName=%s\n" % dataset_name,
                     "DatasetId=%s\n" % dataset_id,
                     "DatasetDescription=%s\n" % make_dataset_description(parts[0], dataset_name),
                     "PatientId=%s\n" % parts[0],
                     "CollectionDate=%s\n" % parts[3]]
            file.writelines(lines)

def make_dir_structure(collection_dir):
    
    # list all sub-directories
    subdirs = [f for f in listdir(collection_dir) if isdir(join(collection_dir, f))]
    for subdir in subdirs:
        
        dataset_name = subdir
        dataset_id = make_dataset_id(dataset_name)
        print "Processing dataset name=%s id=%s" % (dataset_name, dataset_id)
        
        # rename directory to remove troublesome characters
        if not dataset_name == dataset_id:
            srcdir = join(collection_dir,dataset_name)
            dstdir = join(collection_dir,dataset_id)
            print "Renaming sub-directory %s --> %s" % (srcdir, dstdir)
            renames( srcdir, dstdir )
        
        # move DICOM files to version 1/ sub-directory
        versiondir = join(collection_dir, dataset_id, "1")
        if not exists(versiondir):
            print "Moving DICOM files to subdirectory: %s" % versiondir
            mkdir(versiondir)
            
            for file in glob.glob(join(collection_dir, dataset_id)+"/*.dcm"):
                shutil.move(file, versiondir)
            
        
        # create dataset metadata
        make_dataset_metadata_file(collection_dir, dataset_name, dataset_id)
        
    
if __name__ == "__main__":
    
    make_dir_structure(collection_dir)
    

