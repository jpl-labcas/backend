# Python script to reorder the MDAnderson IPMN data before publication

import os
import glob
import shutil

# LABCAS_STAGING --> LABCAS_ARCHIVE directories
STAGING_DIR=os.environ['LABCAS_STAGING'] + "/MD_Anderson_Pancreas_IPMN_images"
ARCHIVE_DIR=os.environ['LABCAS_ARCHIVE'] + "/MD_Anderson_Pancreas_IPMN_images"

# dataset version
version = "1"

# utility function to copy a file from the input to the output directory
def copy_file(inputfile):

  filename = inputfile.split("/")[-1]
  outputfile = "%s/%s" % (odir, filename)
  if not os.path.exists(outputfile):
     print "Copying file: %s --> %s" % (inputfile, outputfile)
     shutil.copy(inputfile, outputfile)

# loop over sub-directories
for isubdir in os.listdir(STAGING_DIR):
    
    idir = "%s/%s/Scan1" % (STAGING_DIR, isubdir)

    # replace blanks
    osubdir = isubdir.replace(' ','_')
    print "Processing subdir: %s --> %s" % (isubdir, osubdir)
    
    # create output directory
    odir = "%s/%s/%s" % (ARCHIVE_DIR, osubdir, version)
    if not os.path.exists(odir):
       os.makedirs(odir)

    # copy qptiff
    for f in glob.glob("%s/*.qptiff" % idir):
        copy_file(f)

    # copy .im3 files
    for f in glob.glob("%s/MSI/*.im3" % idir):
        copy_file(f)


	
