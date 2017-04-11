import os
import sys
from glob import glob
import dicom

# collection root directory:
collection_dir = '/usr/local/labcas/backend/staging/CBIS-DDSM/'

# dataset directory
dataset = sys.argv[1]
dataset_dir = '%s/%s' % (collection_dir, dataset)

# dataset version directory
version_dir = '%s/1' % dataset_dir
if not os.path.exists(version_dir):
    os.makedirs(version_dir)

# loop over DICOM files in dataset directory / DOI
files = [y for x in os.walk(dataset_dir+"/DOI" ) for y in glob(os.path.join(x[0], '*.dcm'))]
for f in files:

   # extract file metadata
   src_path = os.path.abspath(f)
   
   try:
       ds = dicom.read_file(f)
       tag_names = ds.dir()
       #for tag_name in tag_names:
       #   data_element = ds.data_element(tag_name)
          #print data_element
       #   if tag_name != 'PixelData':
       #       print 'key=%s --> value=%s' % (tag_name, data_element.value)
       fid = ds.SOPInstanceUID
    
       # move and rename DICOM file
       dst_path = '%s/%s.dcm' % (version_dir, fid)
       print '\nMoving DICOM file=%s --> %s' % (src_path, dst_path)
       if not os.path.exists(dst_path):
          os.rename(src_path, dst_path)
   except Exception as e:
        print 'Error while processing file: %s' % src_path
        print e
