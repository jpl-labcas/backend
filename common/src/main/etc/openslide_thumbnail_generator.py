# Thumbnail generator for OpenSlide compatible files.

# Usage: python openslide_thumbnail_generator.py <filepath.svs> <thumbnail_root_dir> <thumbnail_root_url>
#
# Example: python /usr/local/labcas_home/etc/openslide_thumbnail_generator.py /usr/local/labcas_archive/Team_37_CTIIP_Animal_Models/CTIIP-1.1b/1/EX10-0061-3N-[NBF]-ER.svs /data/EDRN/thumbnails http://localhost/thumbnails
#
# Will generate a thumbnail named <filepath>.png under the root directory specified in ~/labcas.properties.
# The file location will be returned as "_File_ThumbnailUrl" where the prefix "_File_"
# will be removed before ingestion into the Solr index.

import sys
import os
from openslide import OpenSlide

def build_thumbnail_filepath(image_filepath, thumbnail_root_dir):
    '''
    Build the thumbnail file path as thumbnail_dir/collection/dataset/version/<name>.png
    '''
    
    (dirname, filename) = os.path.split( image_filepath )
    (name, ext) = os.path.splitext(filename)
    dirparts = dirname.split("/")
    if dirparts[-1]=="1":
        subdir = "/".join( dirparts[-3:] )
    else:
        subdir = "/".join(dirparts[-2:].join("/")) + "1/"
    
    thumbnail_dir = os.path.join(thumbnail_root_dir, subdir)
    if not os.path.exists(thumbnail_dir):
        os.makedirs(thumbnail_dir)
    thumb_filepath = os.path.join(thumbnail_dir, name + ".png")
    
    return thumb_filepath

def generate_thumbnail(image_filepath, thumbnail_filepath):
    '''
    Generates a thumbnail from the specified image.
    '''

    # open image with OpenSlide library
    image_file = OpenSlide(image_filepath)
    
    # extract image dimensions
    image_dims = image_file.dimensions
    
    # make thumbnail 100 times smaller
    thumb_dims = tuple( (x/100 for x in image_dims) )
    
    # create thumbnail
    thumb_file = image_file.get_thumbnail(thumb_dims)
    
    # save file with desired path, format
    thumb_file.save(thumbnail_filepath, "png")
    
    # cleanup
    image_file.close()
        

if __name__ == "__main__":
    
    # input arguments
    image_filepath = sys.argv[1]
    thumbnail_root_dir = sys.argv[2]
    thumbnails_root_url = sys.argv[3]
        
    thumbnail_filepath = build_thumbnail_filepath(image_filepath, thumbnail_root_dir)
    
    generate_thumbnail(image_filepath, thumbnail_filepath)
    
    # print out metadata for calling program
    print "FileThumbnailUrl=%s" % thumbnail_filepath.replace(thumbnail_root_dir, thumbnails_root_url)
    
    

