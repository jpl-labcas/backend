# Script to re-organize the NLST data archive of DICOM images

import os
import sys
from glob import glob
from shutil import copyfile
import dicom

# data directories before and after re-organization
source_dir = '/labcas-data/NLST-copy/'
target_dir = '/labcas-data/NLST-copy-processed/'
database_file = '%s/listing.txt' % target_dir

# open database file
dbfile = open(database_file, 'w')
dbfile.write("PatientID StudyInstanceUID SeriesInstanceUID SOPInstanceUID\n")

# loop over source sub-directories
for subdir in os.listdir(source_dir):
  source_subdir = os.path.join(source_dir, subdir)
  print 'Processing sub-directory: %s\n' % source_subdir

  # create target sub-directory/version
  target_subdir = '%s%s/1' % (target_dir, subdir)
  if not os.path.exists(target_subdir):
    os.makedirs(target_subdir)

  # loop recursively over all DICOM files in sub-directory
  src_files = [y for x in os.walk(source_subdir) for y in glob(os.path.join(x[0], '*.dcm'))]
  for src_file in src_files:
    print 'Processing file: %s' % src_file
    
    # extract file metadata
    try:
       ds = dicom.read_file(src_file)
       tag_names = ds.dir()
       #for tag_name in tag_names:
       #   data_element = ds.data_element(tag_name)
          #print data_element
       #   if tag_name != 'PixelData':
       #       print 'key=%s --> value=%s' % (tag_name, data_element.value)
       series_id = ds.SeriesInstanceUID
       study_id = ds.StudyInstanceUID
       file_id = ds.SOPInstanceUID
    
       # rename DICOM file (copy or move)
       dst_file = '%s/%s.dcm' % (target_subdir, file_id)
       if not os.path.exists(dst_file):
         print '\t --> %s' % (dst_file)
       #   os.rename(src_file, dst_file)
       # copyfile(src_file, dst_file)

       # write file hierarchy into database file
       dbfile.write("%s %s %s %s\n" % (subdir, study_id, series_id, dst_file) )

    except Exception as e:
        print 'Error while processing file: %s' % src_file
        print e

    
# clode database file
dbfile.close()
