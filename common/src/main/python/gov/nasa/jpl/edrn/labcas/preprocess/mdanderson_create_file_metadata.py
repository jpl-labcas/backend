# python script to create file-level metadata for MD Anderson data
import os
import glob
from utils import write_metadata

# loop over sub directories, files
root_dir = "/labcas-data/MD_Anderson_Pancreas_IPMN_images"
archive_dirs = [d for d in glob.glob("%s/IPMN-0*" % root_dir)]
for archive_dir in archive_dirs:
    for filename in glob.glob("%s/1/*" % archive_dir):
    
      # example filenames:
      # IPMN P2-01_L05_[56016,14321]_image_with_all_seg.tif
      # IPMN P2-01_L04_[51068,15070].im3
      print filename
      (name,ext)= os.path.splitext(filename)
      if ext == '.met' or ext == '.xmlmet':
         continue
      
      metadata = {}
    
      # build file description
      description = ""
      if ext == '.tif':
         description += "Tif file processed by InForm software."
      elif ext == '.qptiff':
         description += "Perkin Elmer Tif file."
      elif ext == '.im3':
         description += "Original Vectra microscope data."
      elif ext == '.txt':
         description += "Raw data generated from each image."
      elif ext == '.svs':
         description += "Aperio images stained with Hematoxylin and Eosin."
    
      if "_L01_" in filename:
         description += " Low grade lesion area 1."
      elif "_L02_" in filename:
         description += " Low grade lesion area 2."
      elif "_L03_" in filename:
         description += " Low grade lesion area 3."
      elif "_L04_" in filename:
         description += " Low grade lesion area 4."
      elif "_L05_" in filename:
         description += " Low grade lesion area 5."
      elif "_H01_" in filename:
         description += " High grade lesion area 1."
      elif "_H02_" in filename:
         description += " High grade lesion area 2." 
      elif "_H03_" in filename:
         description += " High grade lesion area 3."
      elif "_H04_" in filename:
         description += " High grade lesion area 4."
      elif "_H05_" in filename:
         description += " High grade lesion area 5."
    
      if 'tissue_seg' in filename:
         description += " Tissue segmentation."
      elif 'cell_seg_map' in filename:
         description += " Cell segmentation map."
      elif 'phenotype_map' in filename:
         description += " Phenotype map."
      elif 'binary_seg_maps' in filename:
         description += " Binary segmentation maps."
      elif 'cell_seg_data_summary' in filename:
         description += " Cell segmentation data summary."
      elif 'cell_seg_data' in filename:
         description += " Cell segmentation data."
      elif 'component_data' in filename:
         description += " Component Data."
      elif 'composite_image' in filename:
         description += " Composite image."
      elif 'all_seg' in filename:
         description += " All segments."
      elif 'cell_seg_map' in filename:
         description += " Cell segmentation map."
      elif 'tissue_seg_data_summary' in filename:
         description += " Tissue segmentation data summary."
      elif 'tissue_seg_data' in filename:
         description += " Tissue segmentation data."
    
      metadata['Description'] = description
    
      # Processing level
      if ext == '.tif' or ext == '.svs':
         metadata['ProcessingLevel'] = "processed"
      elif ext == '.im3' or ext == '.txt':
         metadata['ProcessingLevel'] = "raw"
    
      # Panel
      if 'IPMN P1' in filename:
         metadata['Panel'] = "Panel 1: PD-L1, CD68, PD-1, CD8, CD3, AE1/AE3, DAPI"
      elif 'IPMN P2' in filename:
         metadata['Panel'] = "Panel2: CD20, CD45RO,FOXP3, Granzyme B, CD57, AE1/AE3, DAPI"
        
      
      # write out metadata file
      metadata_filepath = os.path.abspath(filename+".xmlmet")
      write_metadata(metadata_filepath, metadata)
