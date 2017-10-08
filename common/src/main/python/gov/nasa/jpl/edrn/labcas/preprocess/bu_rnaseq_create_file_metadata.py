# python script to create file-level metadata for BU RNAseq files
import os
import glob
from utils import write_metadata

archive_dir = "/labcas-data/Boston_University_Lung_Tumor_Sequencing/FFPE_Lung_Tumor_Sequencing/1"

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
  
 
  

