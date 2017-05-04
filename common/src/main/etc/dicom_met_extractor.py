# Metadata extractor for DICOM files
# Usage: python dicom_met_extractor.py <filepath.dcm>
# Will extract metadata into the file: <filemath.dcm>.xmlmet
# All metadata keys are prepended with "_File_" 
# which will be removed before ingestion into the Solr index

import sys
import dicom

dicom_filepath = sys.argv[1]
met_filepath = dicom_filepath + ".xmlmet"

# read input file
ds = dicom.read_file(dicom_filepath)
tag_names = ds.dir()

# extract metadata, write output file
with open(met_filepath,'w') as file: 
   file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')

   # loop over input metadata fields
   for tag_name in tag_names:
      data_element = ds.data_element(tag_name)
      if data_element:
          if tag_name != 'PixelData' and tag_name!= 'LargestImagePixelValue' and tag_name != 'SmallestImagePixelValue': # skip binary data
             #print 'key=%s --> value=%s' % (tag_name, data_element.value)
             file.write('\t<keyval type="vector">\n')
             file.write('\t\t<key>_File_%s</key>\n' % str(tag_name))
             file.write('\t\t<val>%s</val>\n' % str(data_element.value))
             file.write('\t</keyval>\n')

   file.write('</cas:metadata>\n')
