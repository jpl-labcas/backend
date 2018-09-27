# Thumbnail generator for DICOM files
#
# Usage: python openslide_thumbnail_generator.py <filepath.dcm> <thumbnail_root_dir> <thumbnail_root_url>
#
# Example: python dicom_thumbnail_generator.py /labcas-data/CBIS-DDSM/Calc-Training_Full_Mammogram_Images/1/1.3.6.1.4.1.9590.100.1.2.988185913537118118779345241081570907.dcm /data/EDRN/thumbnails http://localhost/thumbnails
#
# Will generate a thumbnail named <filepath>.png under the root directory specified in ~/labcas.properties.
# The file location will be returned as "_File_ThumbnailUrl" where the prefix "_File_"
# will be removed before ingestion into the Solr index.

import sys
import os
import numpy as np
import png
import pydicom
from PIL import Image

from thumbnails import build_thumbnail_filepath

THUMBNAIL_SIZE = 50

def generate_thumbnail(image_filepath, thumbnail_filepath):
    
    # read DICOM dataset
    #dataset = dicom.read_file( image_filepath )
    #size = (dataset.Columns, dataset.Rows)
    
    # build PIL image from dataset
    #img = get_PIL(dataset)
    
    # build thumbnail from image
    #thumb = img.convert('L')
    #thumb.thumbnail( (size[0]/REDUCTION_FACTORT, size[1]/REDUCTION_FACTORT) )
    
    # save file with desired path, format
    #thumb.save(thumbnail_filepath, "png")
        
    ds = pydicom.dcmread(image_filepath)

    shape = ds.pixel_array.shape
    
    # Convert to float to avoid overflow or underflow losses.
    image_2d = ds.pixel_array.astype(float)
    
    # Rescaling grey scale between 0-255
    image_2d_scaled = (np.maximum(image_2d,0) / image_2d.max()) * 255.0
    
    # Convert to uint
    image_2d_scaled = np.uint8(image_2d_scaled)
    
    # Write the PNG file
    with open(thumbnail_filepath, 'wb') as png_file:
        w = png.Writer(shape[1], shape[0], greyscale=True)
        w.write(png_file, image_2d_scaled)

    # Convert current file to thumbnail - will overwrite previous        
    size=THUMBNAIL_SIZE, THUMBNAIL_SIZE*(shape[0]/shape[1])
    im = (Image.open(thumbnail_filepath))
    im = im.resize(size, Image.ANTIALIAS)
    im.save(thumbnail_filepath,"PNG")
         

if __name__ == "__main__":
    
    # input arguments
    image_filepath = sys.argv[1]
    thumbnail_root_dir = sys.argv[2]
    thumbnails_root_url = sys.argv[3]
        
    thumbnail_filepath = build_thumbnail_filepath(image_filepath, thumbnail_root_dir)
    
    generate_thumbnail(image_filepath, thumbnail_filepath)
    
    # print out metadata for calling program
    print "FileThumbnailUrl=%s" % thumbnail_filepath.replace(thumbnail_root_dir, thumbnails_root_url)