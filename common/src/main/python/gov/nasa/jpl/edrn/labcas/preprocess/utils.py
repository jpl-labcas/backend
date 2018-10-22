# Collection of Python utilities for LabCAS operations

def write_description(metadata_filepath, description):
    '''Writes the file description to the ancillary metadata file.'''
    
    print "Writing metadata file: %s" % metadata_filepath
    with open(metadata_filepath,'w') as file: 
        file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
        file.write('\t<keyval type="vector">\n')
        file.write('\t\t<key>_File_Description</key>\n')
        file.write('\t\t<val>%s</val>\n' % description)
        file.write('\t</keyval>\n')
        file.write('</cas:metadata>\n')
        
def write_file_metadata(metadata_filepath, metadata):
    '''Writes file metadata to the ancillary file.'''
    
    print "Writing metadata file: %s" % metadata_filepath
    with open(metadata_filepath,'w') as file: 
        file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
        for key, value in metadata.items():
            file.write('\t<keyval type="vector">\n')
            file.write('\t\t<key>_File_%s</key>\n' % key)
            file.write('\t\t<val>%s</val>\n' % value)
            file.write('\t</keyval>\n')
        file.write('</cas:metadata>\n')
        
def write_dataset_metadata(filepath, metadata):
    '''Writes file metadata to the ancillary file.'''
    
    print "Writing metadata file: %s" % filepath
    with open(filepath,'w') as file: 
        file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
        for key, value in metadata.items():
            file.write('\t<keyval type="vector">\n')
            file.write('\t\t<key>%s</key>\n' % key)
            file.write('\t\t<val>%s</val>\n' % value)
            file.write('\t</keyval>\n')
        file.write('</cas:metadata>\n')