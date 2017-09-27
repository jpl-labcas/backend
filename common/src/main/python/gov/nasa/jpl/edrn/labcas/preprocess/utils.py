# Collection of Python utilities for LabCAS operations

def write_metadata(metadata_filepath, description):
    '''Writes the file description to the ancillary metadata file.'''
    
    print "Writing metadata file: %s" % metadata_filepath
    with open(metadata_filepath,'w') as file: 
        file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
        file.write('\t<keyval type="vector">\n')
        file.write('\t\t<key>_File_Description</key>\n')
        file.write('\t\t<val>%s</val>\n' % description)
        file.write('\t</keyval>\n')
        file.write('</cas:metadata>\n')