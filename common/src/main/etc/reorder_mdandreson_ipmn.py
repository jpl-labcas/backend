# Python script to reorder the MDAnderson IPMN data before publication

import os
import glob
import shutil

# parameters
COLLECTION_NAME = 'MD_Anderson_Pancreas_IPMN_images'

# LABCAS_STAGING --> LABCAS_ARCHIVE directories
STAGING_DIR=os.environ['LABCAS_STAGING'] + "/" + COLLECTION_NAME
ARCHIVE_DIR=os.environ['LABCAS_ARCHIVE'] + "/" + COLLECTION_NAME

# dataset version
version = "1"

# utility function to copy a file from the input to the output directory
def copy_file(inputfile, outputdir):

  filename = inputfile.split("/")[-1]
  outputfile = "%s/%s" % (outputdir, filename)
  if not os.path.exists(outputfile):
     print "Copying file: %s --> %s" % (inputfile, outputfile)
     shutil.copy(inputfile, outputfile)

# loop over sub-directories
for isubdir in os.listdir(STAGING_DIR):
    
    idir = "%s/%s/Scan1" % (STAGING_DIR, isubdir)

    # replace blanks
    dataset_name = isubdir.replace(' ','_')
    print "Processing subdir: %s --> %s" % (isubdir, dataset_name)
    
    # create output directory
    odir = "%s/%s/%s" % (ARCHIVE_DIR, dataset_name, version)
    if not os.path.exists(odir):
       os.makedirs(odir)

    # copy qptiff
    for f in glob.glob("%s/*.qptiff" % idir):
        copy_file(f, odir)

    # copy .im3 files
    for f in glob.glob("%s/MSI/*.im3" % idir):
        copy_file(f, odir)

    # create dataset metadata file
    template_file = os.environ['LABCAS_METADATA'] + "/" + COLLECTION_NAME + "/TEMPLATE.cfg"
    dataset_metadata_file = ARCHIVE_DIR + "/" + dataset_name + "/" + dataset_name + ".cfg"

    if not os.path.exists(dataset_metadata_file):
       print 'Creating dataset metadata file: %s' % dataset_metadata_file

       # read in template metadata file
       with open(template_file) as f:
          metadata = f.read()

       # replace metadata
       metadata = metadata.replace("DATASET_NAME", dataset_name)
       metadata = metadata.replace("DATASET_DESCRIPTION", dataset_name)
    
       # write out metadata
       with open(dataset_metadata_file, 'w') as f:
          f.write(metadata)
