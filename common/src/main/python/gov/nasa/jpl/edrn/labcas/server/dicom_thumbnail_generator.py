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
import dicom
import PIL.Image
import numpy as np

from thumbnails import build_thumbnail_filepath

REDUCTION_FACTORT = 50

# Note: adapted from pydicom/contrib/pydicom_PIL.py
def get_LUT_value(data, window, level):
    """Apply the RGB Look-Up Table for the given data and window/level value."""
    
    return np.piecewise(data,
                        [data <= (level - 0.5 - (window - 1) / 2),
                         data > (level - 0.5 + (window - 1) / 2)],
                        [0, 255, lambda data: ((data - (level - 0.5)) / (window - 1) + 0.5) * (255 - 0)])


# Note: adapted from pydicom/contrib/pydicom_PIL.py
def get_PIL(dataset):
    """Display an image using the Python Imaging Library (PIL)."""

    if ('WindowWidth' not in dataset) or ('WindowCenter' not in dataset):  # can only apply LUT if these values exist
        bits = dataset.BitsAllocated
        samples = dataset.SamplesPerPixel
        if bits == 8 and samples == 1:
            mode = "L"
        elif bits == 8 and samples == 3:
            mode = "RGB"
        elif bits == 16:
            mode = "I;16"  # not sure about this -- PIL source says is 'experimental' and no documentation. Also, should bytes swap depending on endian of file and system??
        else:
            raise TypeError("Don't know PIL mode for %d BitsAllocated and %d SamplesPerPixel" % (bits, samples))

        # PIL size = (width, height)
        size = (dataset.Columns, dataset.Rows)

        im = PIL.Image.frombuffer(mode, size, dataset.PixelData, "raw", mode, 0, 1)  # Recommended to specify all details by http://www.pythonware.com/library/pil/handbook/image.htm

    else:
        image = get_LUT_value(dataset.pixel_array, dataset.WindowWidth, dataset.WindowCenter)
        im = PIL.Image.fromarray(image).convert('L')  # Convert mode to L since LUT has only 256 values: http://www.pythonware.com/library/pil/handbook/image.htm

    return im

def generate_thumbnail(image_filepath, thumbnail_filepath):
    
    # read DICOM dataset
    dataset = dicom.read_file( image_filepath )
    size = (dataset.Columns, dataset.Rows)
    
    # build PIL image from dataset
    img = get_PIL(dataset)
    
    # build thumbnail from image
    thumb = img.convert('L')
    thumb.thumbnail( (size[0]/REDUCTION_FACTORT, size[1]/REDUCTION_FACTORT) )
    
    # save file with desired path, format
    thumb.save(thumbnail_filepath, "png")
    
    # cleanup
    #img.close() # depending on PIL version close() method might not exist


if __name__ == "__main__":
    
    # input arguments
    image_filepath = sys.argv[1]
    thumbnail_root_dir = sys.argv[2]
    thumbnails_root_url = sys.argv[3]
        
    thumbnail_filepath = build_thumbnail_filepath(image_filepath, thumbnail_root_dir)
    
    generate_thumbnail(image_filepath, thumbnail_filepath)
    
    # print out metadata for calling program
    print "FileThumbnailUrl=%s" % thumbnail_filepath.replace(thumbnail_root_dir, thumbnails_root_url)