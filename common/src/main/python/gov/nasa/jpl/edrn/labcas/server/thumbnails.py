# Utility functions for thumbnail GENERATION_RE

import os

def build_thumbnail_filepath(image_filepath, thumbnail_root_dir):
    '''
    Builds the thumbnail file path as thumbnail_dir/collection/dataset/version/<name>.png
    '''
    
    labcas_archive = os.environ.get('LABCAS_ARCHIVE','')
    thumbnail_dir = image_filepath.replace(labcas_archive, thumbnail_root_dir)
    if not os.path.exists(thumbnail_dir):
        os.makedirs(thumbnail_dir)

    thumb_filepath = thumbnail_dir + ".png"
    return thumb_filepath