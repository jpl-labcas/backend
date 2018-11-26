import sys
import pydicom
import re
import os
from utils import write_file_metadata

# list of DICOM metadata fields taht are NOT extracted because they have binary value, or bad characters, or are too long
IGNORED_TAGS = ["PixelData", "LargestImagePixelValue", "SmallestImagePixelValue", "PerFrameFunctionalGroupsSequence",
                "ROIContourSequence",
                "RedPaletteColorLookupTableData", "BluePaletteColorLookupTableData", "GreenPaletteColorLookupTableData"]

DICOM_COLLECTIONS = ["Automated_System_For_Breast_Cancer_Biomarker_Analysis",
                     "Combined_Imaging_and_Blood_Biomarkers_for_Breast_Cancer_Diagnosis"]

NAMESPACE_DICOM = "labcas.dicom"
NAMESPACE_RADIOLOGY = "labcas.radiology"

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
    ds = pydicom.read_file(dicom_filepath)
    tag_names = ds.dir()
    
    # loop over input metadata fields
    for tag_name in tag_names:
        data_element = ds.data_element(tag_name)
        if data_element:
            if tag_name not in IGNORED_TAGS:
                #print 'key=%s --> value=%s' % (tag_name, data_element.value)
                metadata["_File_%s:%s" % (NAMESPACE_DICOM, str(tag_name))] = str(data_element.value)

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

def get_top_dataset_name(inst, patient_number):
    
    # patient
    if inst == 'D':
        dataset_name = "Duke patient # %s" % patient_number
    elif inst == 'E':
        dataset_name = "Moffitt patient # %s" % patient_number
    elif inst == 'C':
        dataset_name = "Case # %s (unilateral breast cancer)" % patient_number
    elif inst == 'N':
        dataset_name = "Case # %s (control i.e. no cancer)" % patient_number
    else:
        dataset_name = "Patient # %s" % patient_number
    
    return dataset_name
    
def _extract_metadata_from_filepath_bi(inst, patient_number, 
                                       image_type=None,
                                       image_modality=None,
                                       image_orientation=None,
                                       processing_level=None,
                                       bi_rads_score=None,
                                       lesion_number=None,
                                       volume_type=None,
                                       slice_number=None,
                                       total_number_of_slices=None):
    
    metadata = {}
        
    # patient
    description = get_top_dataset_name(inst, patient_number)
        
    # image type
    if image_type:
        metadata["_File_"+NAMESPACE_RADIOLOGY+":image_type"] = image_type
        if image_type.upper() == 'MG':
            description += " Mammography"
        elif image_type.upper() == 'BT':
            description += " Breast tomosynthesis"
        elif image_type.upper() == 'TRU':
            description += " Truth File"
        elif image_type.upper() == 'MASK':
            description += "Mask File"
            
    # image modality
    if image_modality:
        metadata["_File_"+NAMESPACE_RADIOLOGY+":image_modality"] = image_modality
        if image_modality.upper() == 'FFDM':
            description += ", Full Field Digital Mammography (FFDM)"
        elif image_modality.upper() == 'DBT':
            description += ", Digital Breast Tomosynthesis (DBT)"
        elif image_modality.upper() == 'SYN':
            description += ", Synthetic 2D Full Field Digital Mammography (FFDM)"
        
    # image orientation
    if image_orientation:
        metadata["_File_"+NAMESPACE_RADIOLOGY+":image_orientation"] = image_orientation
        if image_orientation.upper() == 'LMLO':
            description += ', Orientation: left mediolateral oblique'
        elif image_orientation.upper() == 'LCC':
            description += ', Orientation: left craniocaudal'
        elif image_orientation.upper() == 'RMLO':
            description += ', Orientation: right mediolateral oblique'
        elif image_orientation.upper() == 'RCC':
            description += ', Orientation: right craniocaudal'
            
    # processing level
    if processing_level:
        metadata["_File_"+NAMESPACE_RADIOLOGY+":processing_level"] = processing_level
        if processing_level.upper() == "DAT":
            description += ', raw data'
        elif processing_level.upper() == "PRO":
            description += ', processed data'
    
    # BI-RADS score
    if bi_rads_score:
        metadata["_File_"+NAMESPACE_RADIOLOGY+":bi-rads_score"] = bi_rads_score
        if bi_rads_score.upper() == '4A':
            description += ", Low suspicion for malignancy"
        elif bi_rads_score.upper() == '4B':
            description += ", Moderate suspicion for malignancy"
        elif bi_rads_score.upper() == '4C':
            description += ", High suspicion for malignancy"
        elif bi_rads_score.upper() == '5':
            description += ", Highly suggestive of malignancy"
        elif bi_rads_score.upper() == 'F':
            description += ", Not biopsied but instead presumed benign based on follow-up imaging"
        
    # lesion number
    if lesion_number:
        metadata["_File_"+NAMESPACE_RADIOLOGY+":lesion_number"] = lesion_number
        description += ", Lesion # %s" % lesion_number
        
    # slide number and total number of slices
    if slice_number:
        metadata["_File_"+NAMESPACE_RADIOLOGY+":slice_number"] = slice_number
        description += ", Slice # %s" % slice_number
        
        if total_number_of_slices:
            metadata[NAMESPACE_RADIOLOGY+":total_number_of_slices"] = total_number_of_slices
            description += " out of %s" % total_number_of_slices
            
    # volume type
    if volume_type:
        metadata["_File_"+NAMESPACE_RADIOLOGY+":volume_type"] = volume_type
        if volume_type == "VOL":
             description += ", Packed volume"
        elif volume_type == "UV":
             description += ", Unpacked volume"
       
    # long description
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
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="MG",
                                                  image_modality="SYN",
                                                  image_orientation=match.group(3),
                                                  processing_level=None,
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type=None,
                                                  slice_number=None,
                                                  total_number_of_slices=None)
    match = re.search("(\w)(\d+)_BT_SYN_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="BT",
                                                  image_modality="SYN",
                                                  image_orientation=match.group(3),
                                                  processing_level=None,
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type=None,
                                                  slice_number=None,
                                                  total_number_of_slices=None)


    
    # Mammograms
    # Format:    ID_MG_DAT_VIEW.dcm    ID_MG_PRO_VIEW.dcm
    # Example:   E0001_MG_DAT_LCC.dcm  E0001_MG_PRO_LCC.dcm
    match = re.search("(\w)(\d+)_MG_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="MG",
                                                  image_modality="FFDM",
                                                  image_orientation=match.group(4),
                                                  processing_level=match.group(3),
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type=None,
                                                  slice_number=None,
                                                  total_number_of_slices=None)
        
    # Example: C0001_MASK_PRO_LCC.dcm
    match = re.search("(\w)(\d+)_MASK_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="Mask",
                                                  image_modality="FFDM",
                                                  image_orientation=match.group(4),
                                                  processing_level=match.group(3),
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type=None,
                                                  slice_number=None,
                                                  total_number_of_slices=None)

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
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="Tru",
                                                  image_modality="FFDM",
                                                  image_orientation=match.group(6),
                                                  processing_level=match.group(5),
                                                  bi_rads_score=match.group(3),
                                                  lesion_number=match.group(4),
                                                  volume_type=None,
                                                  slice_number=None,
                                                  total_number_of_slices=None)
        
    # Non-biopsied findings
    # E0001_TRU_F1_DAT_LCC
    # E0001_TRU_F2_DAT_LCC
    match = re.search("(\w)(\d+)_TRU_F(\d+)_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type_long = "2D Truth file for non-biopsied lesion (presumed benign)"
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="Tru",
                                                  image_modality="FFDM",
                                                  image_orientation=match.group(5),
                                                  processing_level=match.group(4),
                                                  bi_rads_score="F",
                                                  lesion_number=match.group(3),
                                                  volume_type=None,
                                                  slice_number=None,
                                                  total_number_of_slices=None)
        
    # Packed Volumes
    # Format: ID_BT_VOL_VIEW.dcm
    # Example: E0001_BT_VOL_LCC.dcm
    match = re.search("(\w)(\d+)_BT_VOL_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="BT",
                                                  image_modality="BDT",
                                                  image_orientation=match.group(3),
                                                  processing_level=None,
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type="VOL",
                                                  slice_number=None,
                                                  total_number_of_slices=None)
    
    # Unpacked Volumes
    # Format: ID_BT_UV_SLICE-NUMBER_TOTAL-NUMBER-SLICES_VIEW.dcm
    # Example: E0001_BT_UV_002_085_LCC.dcm (2nd slice from 85 slices).
    match = re.search("(\w)(\d+)_BT_UV_(\d+)_(\d+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="BT",
                                                  image_modality="BDT",
                                                  image_orientation=match.group(5),
                                                  processing_level=None,
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type="UV",
                                                  slice_number=match.group(3),
                                                  total_number_of_slices=match.group(4))

    # Truth Files for Volumes and Synthetic Mammograms
    # Example: E0001_TRU_VOL_002_LCC.dcm (2nd virtual slice in the packed volume).
    match = re.search("(\w)(\d+)_TRU_VOL_(\d+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="Tru",
                                                  image_modality="BDT",
                                                  image_orientation=match.group(4),
                                                  processing_level=None,
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type="VOL",
                                                  slice_number=match.group(3),
                                                  total_number_of_slices=None)

    # Example: E0001_TRU_UV_002_LCC.dcm (2nd slice image out of many unpacked slices).
    match = re.search("(\w)(\d+)_TRU_UV_(\d+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="Tru",
                                                  image_modality="BDT",
                                                  image_orientation=match.group(4),
                                                  processing_level=None,
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type="UV",
                                                  slice_number=match.group(3),
                                                  total_number_of_slices=None)


    # Truth file naming for the SYNs follows the 2D mammogram examples. 
    # E0001_TRU_SYN_11_DAT_LCC.dcm
    match = re.search("(\w)(\d+)_TRU_SYN_(\d+)_(\w+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        image_type_long = "2D Truth file for synthetic slice # %s" % slice_number
        return _extract_metadata_from_filepath_bi(inst, patient_number,
                                                  image_type="Tru",
                                                  image_modality="SYN",
                                                  image_orientation=match.group(5),
                                                  processing_level=match.group(4),
                                                  bi_rads_score=None,
                                                  lesion_number=None,
                                                  volume_type=None,
                                                  slice_number=match.group(3),
                                                  total_number_of_slices=None)
    
    
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
