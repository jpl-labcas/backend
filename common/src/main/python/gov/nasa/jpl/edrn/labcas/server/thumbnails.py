# Utility functions for thumbnail GENERATION_RE

import os

def build_thumbnail_filepath(image_filepath, thumbnail_root_dir):
    '''
    Builds the thumbnail file path as thumbnail_dir/collection/dataset/version/<name>.png
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