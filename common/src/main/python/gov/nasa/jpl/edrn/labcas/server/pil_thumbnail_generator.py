# Generic thumbnail generator based on the Python Image Library (PIL)
#
# Usage: python pil_thumbnail_generator.py <filepath> <thumbnail_root_dir> <thumbnail_root_url>
#
# Example: python pil_thumbnail_generator.py /usr/local/labcas_archive/My_Data_Collection/Best_Dataset/1/Cinquini_Dulles_June_2017.tiff /Users/cinquini/tmp/thumbnails http://localhost/thumbnails
#
# Will generate a thumbnail named <filepath>.png under the root directory specified in ~/labcas.properties.
# The file location will be returned as "_File_ThumbnailUrl" where the prefix "_File_"
# will be removed before ingestion into the Solr index.

import sys
import os
from PIL import Image

from thumbnails import build_thumbnail_filepath

#REDUCTION_FACTOR = 50
THUMBNAIL_SIZE = (100,100)

def generate_thumbnail(image_filepath, thumbnail_filepath):
    
    image = Image.open(image_filepath)
    #image.thumbnail( (image.size[0]/REDUCTION_FACTOR, image.size[1]/REDUCTION_FACTOR) )
    image.thumbnail( THUMBNAIL_SIZE )
    image.save(thumbnail_filepath,"PNG")
    

if __name__ == "__main__":
    
    # input arguments
    image_filepath = sys.argv[1]
    thumbnail_root_dir = sys.argv[2]
    thumbnails_root_url = sys.argv[3]
        
    thumbnail_filepath = build_thumbnail_filepath(image_filepath, thumbnail_root_dir)
    
    generate_thumbnail( image_filepath, thumbnail_filepath)
    
    # print out metadata for calling program
    print "FileThumbnailUrl=%s" % thumbnail_filepath.replace(thumbnail_root_dir, thumbnails_root_url)