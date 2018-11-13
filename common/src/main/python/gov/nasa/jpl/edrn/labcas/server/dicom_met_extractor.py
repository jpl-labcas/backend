import sys
import dicom
from utils import write_file_metadata, extract_metadata_from_filepath

# list of DICOM metadata fields taht are NOT extracted because they have binary value, or bad characters, or are too long
IGNORED_TAGS = ["PixelData", "LargestImagePixelValue", "SmallestImagePixelValue", "PerFrameFunctionalGroupsSequence",
                "ROIContourSequence",
                "RedPaletteColorLookupTableData", "BluePaletteColorLookupTableData", "GreenPaletteColorLookupTableData"]

def extract_metadata(dicom_filepath):
    ''' 
    Metadata extractor for DICOM files.
    Usage: python dicom_met_extractor.py <filepath.dcm>
    Will extract metadata into the file: <filemath.dcm>.xmlmet.
    All metadata keys are prepended with "_File_" 
    which will be removed before ingestion into the Solr index
    '''

    metadata = {}
    
    # extract metadata from file path
    _metadata = extract_metadata_from_filepath(dicom_filepath)
    for (key, value) in _metadata:
        metadata[_key] = value
        
    # add metadata extracted from DICOM header
    ds = dicom.read_file(dicom_filepath)
    tag_names = ds.dir()
    
    # loop over input metadata fields
    for tag_name in tag_names:
        data_element = ds.data_element(tag_name)
        if data_element:
            if tag_name not in IGNORED_TAGS:
                #print 'key=%s --> value=%s' % (tag_name, data_element.value)
                metadata["_File_%s" % str(tag_name)] = str(data_element.value)
                 
    # write out metadata to file
    met_filepath = dicom_filepath + ".xmlmet"
    write_file_metadata(metadata, met_filepath)

if __name__ == "__main__":
    '''
    Example invocation: 
    python gov/nasa/jpl/edrn/labcas/server/dicom_met_extractor.py /usr/local/labcas_archive/Sample_Mammography_Reference_Set/D0001/1/D0001_VOL_RMLO.dcm
    '''
    
    dicom_filepath = sys.argv[1]
    extract_metadata( dicom_filepath )
    