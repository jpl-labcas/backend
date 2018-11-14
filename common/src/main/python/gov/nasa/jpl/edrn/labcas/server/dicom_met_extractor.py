import sys
import dicom
import re
import os
from utils import write_file_metadata

# list of DICOM metadata fields taht are NOT extracted because they have binary value, or bad characters, or are too long
IGNORED_TAGS = ["PixelData", "LargestImagePixelValue", "SmallestImagePixelValue", "PerFrameFunctionalGroupsSequence",
                "ROIContourSequence",
                "RedPaletteColorLookupTableData", "BluePaletteColorLookupTableData", "GreenPaletteColorLookupTableData"]

DICOM_COLLECTIONS = ["Automated_System_For_Breast_Cancer_Biomarker_Analysis",
                     "Combined_Imaging_and_Blood_Biomarkers_for_Breast_Cancer_Diagnosis"]

def extract_metadata(dicom_filepath):
    ''' 
    Metadata extractor for DICOM files.
    Will extract metadata into the file: <filemath.dcm>.xmlmet.
    All metadata keys are prepended with "_File_" 
    which will be removed before ingestion into the Solr index
    '''

    metadata = {}
    
    # extract metadata from file path
    _metadata = extract_metadata_from_filepath(dicom_filepath)
    for (key, value) in _metadata.items():
        metadata[key] = value
        
    # extracts metadata from DICOM header
    _metadata = extract_metadata_from_header(dicom_filepath)
    for (key, value) in _metadata.items():
        metadata[key] = value
                 
    # write out metadata to file
    met_filepath = dicom_filepath + ".xmlmet"
    write_file_metadata(metadata, met_filepath)


def extract_metadata_from_header(dicom_filepath):
    '''
    Extracts metadata from the DICOM header
    '''
    
    metadata = {}
    ds = dicom.read_file(dicom_filepath)
    tag_names = ds.dir()
    
    # loop over input metadata fields
    for tag_name in tag_names:
        data_element = ds.data_element(tag_name)
        if data_element:
            if tag_name not in IGNORED_TAGS:
                #print 'key=%s --> value=%s' % (tag_name, data_element.value)
                metadata["_File_%s" % str(tag_name)] = str(data_element.value)

    return metadata

def extract_metadata_from_filepath(dicom_filepath):
    '''
    Creates metadata from the filepath 
    based on collection specific semantics.
    '''
    
    dicom_filename = os.path.basename(dicom_filepath)
    
    for dicom_coll in DICOM_COLLECTIONS:
        if dicom_coll in dicom_filepath:
            return extract_metadata_from_filepath_bi(dicom_filename)
    
    # no match
    return {}
    
def _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view,
                                       processing_level=None,
                                       birads_rating=None, 
                                       lesion_number=None,
                                       slice_number=None,
                                       total_number_of_slices=None):
        
    description = ""
    
    # patient
    if inst == 'D':
        description += "Duke patient # %s" % patient_number
    elif inst == 'E':
        description += "Moffitt patient # %s" % patient_number
    elif inst == 'C':
        description += "Case # %s (unilateral breast cancer)" % patient_number
    elif inst == 'N':
        description += "Case # %s (control i.e. no cancer)" % patient_number
    else:
        description += "Patient # %s" % patient_number
    
    # image type
    description +=", %s" % image_type
    
    # biopsy rating and lesion number
    if birads_rating:
        description += ", BI-RADS rating: %s" % birads_rating
    if lesion_number:
        description += ", lesion # %s" % lesion_number
        
    # slice number
    if slice_number and total_number_of_slices:
        description += ", slice # %s of %s" % (slice_number, total_number_of_slices)
    
    # view aka orientation
    description += ", orientation: %s" % view
    if view == 'RCC':
        description += " (right craniocaudal)"
    elif view == 'RMLO':
        description += " (right mediolateral oblique)"
    elif view == 'RML':
        description += " (right mediolateral)"
    elif view == 'LCC':
        description += " (left craniocaudal)"
    elif view == 'LLMLO':
        description += " (left mediolateral oblique)"
        
    # processing level
    if processing_level:
        if processing_level == 'DAT':
            description += ', raw image (for processing)'
        elif processing_level == 'PRO':
            description += ', processed image (for display)'
    
    # assemble metadata
    metadata = {}
    metadata["_File_Description"] = description
    return metadata

def extract_metadata_from_filepath_bi(filename):
    
    # match to regular expressions
    
    # Synthetic Mammograms (IMPORTANT: MUST COME BEFORE THE NEXT MATCH)
    # Format: ID_MG_SYN_VIEW.dcm
    # Example: E0001_MG_SYN_LCC.dcm
    match = re.search("(\w)(\d+)_MG_SYN_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type = "2D synthetic mammogram from 3D digital tomosynthesis"
        view = match.group(3)
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view)
    match = re.search("(\w)(\d+)_BT_SYN_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type = "2D synthetic mammogram from 3D digital tomosynthesis"
        view = match.group(3)
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view)

    
    # Mammograms
    # Format:    ID_MG_DAT_VIEW.dcm    ID_MG_PRO_VIEW.dcm
    # Example:   E0001_MG_DAT_LCC.dcm  E0001_MG_PRO_LCC.dcm
    match = re.search("(\w)(\d+)_MG_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type = "2D full-field digital mammography (FFDM)"
        processing_level = match.group(3)
        view = match.group(4)
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view,
                                                  processing_level=processing_level)
        
    # Example: C0001_MASK_PRO_LCC.dcm
    match = re.search("(\w)(\d+)_MASK_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type = "2D full-field digital mammography (FFDM) mask"
        processing_level = match.group(3)
        view = match.group(4)
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view,
                                                  processing_level=processing_level)
    
    # Truth Files for 2D Mammograms
    
    # Biopsied lesions
    # E0100_TRU_4A_1_DAT_LCC.dcm
    # E0100_TRU_4A_1_DAT_LMLO.dcm
    # E0100_TRU_4A_1_DAT_LCC.dcm
    # E0100_TRU_4A_1_DAT_LMLO.dcm
    # E0100_TRU_4B_2_DAT_LCC.dcm
    # E0100_TRU_4B_2_DAT_LMLO.dcm
    # E0100_TRU_4A_1_DAT_LCC.dcm
    # E0100_TRU_4A_1_DAT_LMLO.dcm
    # E0100_TRU_4A_2_DAT_RCC.dcm
    # E0100_TRU_4A_2_DAT_RMLO.dcm
    match = re.search("(\w)(\d+)_TRU_(\w\w)_(\d)_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type = "2D Truth file for biopsied lesion"
        birads_rating = match.group(3)
        lesion_number = match.group(4)
        processing_level = match.group(5)
        view = match.group(6)
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view, 
                                                  processing_level=processing_level,
                                                  birads_rating=birads_rating, lesion_number=lesion_number)
        
    # Non-biopsied findings
    # E0001_TRU_F1_DAT_LCC
    # E0001_TRU_F2_DAT_LCC
    match = re.search("(\w)(\d+)_TRU_F(\d+)_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type = "2D Truth file for non-biopsied lesion (presumed benign)"
        lesion_number = match.group(3)
        processing_level = match.group(4)
        view = match.group(5)
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view, 
                                                  processing_level=processing_level,
                                                  lesion_number=lesion_number)
        
    # Packed Volumes
    # Format: ID_BT_VOL_VIEW.dcm
    # Example: E0001_BT_VOL_LCC.dcm
    match = re.search("(\w)(\d+)_BT_VOL_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type = "3D digital breast tomosynthesis (DBT), packed volume"
        view = match.group(3)
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view)
    
    # Unpacked Volumes
    # Format: ID_BT_UV_SLICE-NUMBER_TOTAL-NUMBER-SLICES_VIEW.dcm
    # Example: E0001_BT_UV_002_085_LCC.dcm (2nd slice from 85 slices).
    match = re.search("(\w)(\d+)_BT_UV_(\d+)_(\d+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type = "3D digital breast tomosynthesis (DBT), un-packed volume"
        slice_number = match.group(3)
        total_number_of_slices = match.group(4)
        view = match.group(5)
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view,
                                                  slice_number=slice_number,
                                                  total_number_of_slices=total_number_of_slices)

    # Truth Files for Volumes and Synthetic Mammograms
    # Example: E0001_TRU_VOL_002_LCC.dcm (2nd virtual slice in the packed volume).
    match = re.search("(\w)(\d+)_TRU_VOL_(\d+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        slice_number = match.group(3)
        view = match.group(4)
        image_type = "2D Truth file for virtual slice # %s from 3D packed volume" % slice_number
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view)
    # Example: E0001_TRU_UV_002_LCC.dcm (2nd slice image out of many unpacked slices).
    match = re.search("(\w)(\d+)_TRU_UV_(\d+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        slice_number = match.group(3)
        view = match.group(4)
        image_type = "2D Truth file for virtual slice # %s from 3D unpacked volume" % slice_number
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view)

    # Truth file naming for the SYNs follows the 2D mammogram examples. 
    # E0001_TRU_SYN_11_DAT_LCC.dcm
    match = re.search("(\w)(\d+)_TRU_SYN_(\d+)_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        slice_number = match.group(3)
        processing_level = match.group(4)
        view = match.group(5)
        image_type = "2D Truth file for synthetic slice # %s" % slice_number
        return _extract_metadata_from_filepath_bi(inst, patient_number, image_type, view,
                                                  processing_level=processing_level)
    
    
    # no match: return empty metadata
    return {}        
    

if __name__ == '__main__':
    '''
    IMPORTANT: this main program is invoked during publication of DICOM files to extract file-level metadata to .xmlmet files.
    DO NOT REMOVE!
    '''
    
    '''
    for filename in ["E0001_MG_DAT_LCC.dcm",
                     "E0001_MG_PRO_LCC.dcm",
                     "E0100_TRU_4A_2_DAT_RMLO.dcm",
                     "E0100_TRU_4A_1_DAT_LMLO.dcm",
                     "E0001_TRU_F1_DAT_LCC.dcm",
                     "E0001_BT_VOL_LCC.dcm",
                     "E0001_BT_UV_002_085_LCC.dcm",
                     "E0001_MG_SYN_LCC.dcm",
                     "E0001_TRU_VOL_002_LCC.dcm",
                     "E0001_TRU_UV_002_LCC.dcm",
                     "E0001_TRU_SYN_11_DAT_LCC.dcm",
                     "E0001_BT_SYN_RCC.dcm",
                     "E0001_BT_SYN_RML.dcm",
                     "C0001_MG_DAT_LCC.dcm",
                     "C0001_MASK_PRO_LCC.dcm",
                     "C0001_MG_DAT_RMLO.dcm",
                     ]:
        metadata = extract_metadata_from_filepath_bi(filename)    
        print(metadata)
        '''
    
        dicom_filepath = sys.argv[1]
        extract_metadata( dicom_filepath )
