# python script to create file-level metadata for BU RNAseq files
import os
import glob

archive_dir = "/labcas-data/Boston_University_Lung_Tumor_Sequencing/FFPE_Lung_Tumor_Sequencing/1"

def write_metadata(metadata_filepath, description):
  print "Writing metadata file: %s" % metadata_filepath
  with open(metadata_filepath,'w') as file: 
    file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
    file.write('\t<keyval type="vector">\n')
    file.write('\t\t<key>_File_Description</key>\n')
    file.write('\t\t<val>%s</val>\n' % description)
    file.write('\t</keyval>\n')
    file.write('</cas:metadata>\n')


# loop over products
for file in glob.glob("%s/*.fastq.gz" % archive_dir):

  # example filepath: 915_S13_L004_R2_001.fastq
  filename = os.path.basename(file).replace(".fastq.gz","")

  # example:  ['9475', 'S14', 'L004', 'R2', '001']
  parts = filename.split("_")
  description = "Sample id=%s, sample index=%s, lane=%s, read=%s, file number=%s" % tuple(parts)
  
  # write out metadata file
  metadata_filepath = os.path.abspath(file+".xmlmet")
  write_metadata(metadata_filepath, description)
  
 
  

