import sys
import dicom
from utils import write_metadata, make_file_description

def extract_metadata( dicom_filepath ):
    ''' 
    Metadata extractor for DICOM files.
    Usage: python dicom_met_extractor.py <filepath.dcm>
    Will extract metadata into the file: <filemath.dcm>.xmlmet.
    All metadata keys are prepended with "_File_" 
    which will be removed before ingestion into the Solr index
    '''

    # read input file, extract metadata
    metadata = {}
    ds = dicom.read_file(dicom_filepath)
    tag_names = ds.dir()
    
    # add file description
    metadata["_File_Description"] = make_file_description( dicom_filepath )
    
    # loop over input metadata fields
    for tag_name in tag_names:
        data_element = ds.data_element(tag_name)
        if data_element:
            if tag_name != 'PixelData' and tag_name!= 'LargestImagePixelValue' and tag_name != 'SmallestImagePixelValue': # skip binary data
                #print 'key=%s --> value=%s' % (tag_name, data_element.value)
                metadata["_File_%s" % str(tag_name)] = str(data_element.value)
                 
    # write out metadata to file
    met_filepath = dicom_filepath + ".xmlmet"
    write_metadata(metadata, met_filepath)

if __name__ == "__main__":
    '''
    Example invocation: 
    python gov/nasa/jpl/edrn/labcas/server/dicom_met_extractor.py /usr/local/labcas_archive/Sample_Mammography_Reference_Set/D0001/1/D0001_VOL_RMLO.dcm
    '''
    
    dicom_filepath = sys.argv[1]
    extract_metadata( dicom_filepath )
    