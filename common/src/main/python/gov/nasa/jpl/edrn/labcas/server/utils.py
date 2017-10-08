# Collection of Python utilities for LabCAS operations

def write_metadata(metadata_dict, metadata_filepath):
    '''Writes out the metadata dictionary to the given metadata file path.'''
    
    print "Writing metadata file: %s" % metadata_filepath
    with open(metadata_filepath,'w') as file: 
        file.write('<cas:metadata xmlns:cas="http://oodt.jpl.nasa.gov/1.0/cas">\n')
        
        for key, value in metadata_dict.iteritems():
            file.write('\t<keyval type="vector">\n')
            file.write('\t\t<key>%s</key>\n' % key)
            file.write('\t\t<val>%s</val>\n' % value)
            file.write('\t</keyval>\n')
            
        file.write('</cas:metadata>\n')