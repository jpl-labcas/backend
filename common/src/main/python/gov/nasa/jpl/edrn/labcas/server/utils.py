# Collection of Python utilities for LabCAS operations
import os
import re

def write_file_metadata(metadata_dict, metadata_filepath):
    '''Writes out the metadata dictionary to the given metadata file path.'''
    
    print("Writing metadata file: %s" % metadata_filepath)
    with open(metadata_filepath,'w') as file: 
        file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
        
        for key, value in metadata_dict.iteritems():
            # do not write out None or empty values
            if value:
                file.write('\t<keyval type="vector">\n')
                file.write('\t\t<key>%s</key>\n' % key)
                file.write('\t\t<val>%s</val>\n' % value)
                file.write('\t</keyval>\n')
            
        file.write('</cas:metadata>\n')    
        
def make_file_description_moffitt( filename ):
    '''Generates the file description using project specific semantics.'''
    
    # split filename
    parts = filename.split("_")
    
    # patient #
    if (parts[0][0]=='D'):
        description = 'Duke patient #%s' % parts[0][1:]
    else:
        description = 'Moffitt patient #%s' % parts[0][1:]
        
    if "_MG_" in filename:
        description += ", mammography 2D"
        
    # packed/unpacked volume
    if parts[1]=='UV':
        description += ", unpacked volume frame # %s/%s" % (parts[2],parts[3])
    elif parts[1] == 'VOL':
        description += ", packed volume"
    # orientation
    for orientation in ['RCC','LCC','LMLO','RMLO']:
        if orientation in filename:
            description += ", orientation: %s" % orientation
            if orientation == 'RCC':
                description += " (Right craniocaudal)"
            elif orientation == 'RMLO':
                description += " (Right mediolateral oblique)"
            elif orientation == 'LCC':
                description += " (Left craniocaudal)"
            elif orientation == 'LLMLO':
                description += " (Left mediolateral oblique)"
        
    # image type
    if 'DAT' in filename:
        description += ", image type: raw"
    elif 'PRO' in filename:
        description += ", image type: processed"
    elif 'CV' in filename:
        description += ", image type: C-View"
    elif 'TRU' in filename:
        description += ", image type: Truth"

    return description
        
def make_file_description(dicom_filepath):
    '''Creates a file description based on collection specific semantics.'''
    
    dicom_filename = os.path.basename(dicom_filepath)
    
    if "Sample_Mammography_Reference_Set" in dicom_filepath:
        return make_file_description_moffitt(dicom_filename)
    
    else:
        return None
    
def _make_file_description_bi(inst, patient_number, image_type, view,
                              processing_level=None,
                              birads_rating=None, 
                              lesion_number=None,
                              slice_number=None,
                              total_number_of_slices=None):
        
    # patient
    if inst == 'D':
        institution = 'Duke'
    else:
        institution = 'Moffitt'
    description = "%s patient # %s" % (institution, patient_number)
    
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
    
    return description

def make_file_description_bi(filename):
    
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
        return _make_file_description_bi(inst, patient_number, image_type, view)

    
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
        return _make_file_description_bi(inst, patient_number, image_type, view,
                                         processing_level=processing_level,)
    
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
        return _make_file_description_bi(inst, patient_number, image_type, view, 
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
        return _make_file_description_bi(inst, patient_number, image_type, view, 
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
        return _make_file_description_bi(inst, patient_number, image_type, view)
    
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
        return _make_file_description_bi(inst, patient_number, image_type, view,
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
        return _make_file_description_bi(inst, patient_number, image_type, view)
    # Example: E0001_TRU_UV_002_LCC.dcm (2nd slice image out of many unpacked slices).
    match = re.search("(\w)(\d+)_TRU_UV_(\d+)_(\w+)\.dcm", filename)
    if match:
        inst = match.group(1)
        patient_number = match.group(2)
        slice_number = match.group(3)
        view = match.group(4)
        image_type = "2D Truth file for virtual slice # %s from 3D unpacked volume" % slice_number
        return _make_file_description_bi(inst, patient_number, image_type, view)

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
        return _make_file_description_bi(inst, patient_number, image_type, view,
                                         processing_level=processing_level)
    
    
    # no match
    return filename


if __name__ == '__main__':
    
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
                     ]:
        desc = make_file_description_bi(filename)    
        print(desc)