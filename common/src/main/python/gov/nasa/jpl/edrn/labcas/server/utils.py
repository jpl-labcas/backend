# Collection of Python utilities for LabCAS operations
import os

def write_file_metadata(metadata_dict, metadata_filepath):
    '''Writes out the metadata dictionary to the given metadata file path.'''
    
    print "Writing metadata file: %s" % metadata_filepath
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
        description = 'Dummy patient #%s' % parts[0][1:]
    else:
        description = 'Real patient #%s' % parts[0][1:]
        
    # packed/unpacked volume
    if parts[1]=='UV':
        description += ", unpacked volume frame # %s/%s" % (parts[2],parts[3])
    elif parts[1] == 'VOL':
        description += ", packed volume"
    # orientation
    for orientation in ['RCC','LCC','LMLO','RMLO']:
        if orientation in filename:
            description += ", orientation: %s" % orientation
            
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