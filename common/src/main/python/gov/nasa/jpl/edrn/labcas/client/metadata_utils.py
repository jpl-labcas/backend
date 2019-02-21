# Metadata utility functions
    
import os
import sys
import logging
import ConfigParser
from xml.sax.saxutils import escape

def _process_value(value):
    
    evalue = escape(value)
    if "|" in evalue:
        # transform into a list
        evalue = evalue.split("|")
    return evalue
    
def read_config_metadata(directory_path):
    '''
    Reads metadata from the same name configuration file found in a directory.
    '''
    
    metadata = {}
    (parent_dir, this_dir) = os.path.split(directory_path)
    config_file_path = directory_path + "/" + this_dir + ".cfg"
    
    if os.path.exists(config_file_path):
        
        logging.debug("Reading configuration file: %s" % config_file_path)
        
        config = ConfigParser.ConfigParser()
        # must set following line explicitly to preserve the case of configuration keys
        config.optionxform = str 
        
        try:
            config.read(config_file_path)
            for section in config.sections():
                for key, value in config.items(section):
                    # must escape XML reserved characters: &<>"
                    pvalue = _process_value(value)
                    logging.debug('\t%s = %s' % (key, pvalue))
                    # [Dataset] section: prefix all fields with 'Dataset:'
                    if section=='Dataset' and not key.startswith('Dataset'):
                        metadata['Dataset:%s' % key] = pvalue
                    else:
                        metadata[key] = pvalue
        except Exception as e:
            logging.error("ERROR reading metadata configuration")
            logging.error(e)
            sys.exit(-1)
    
    return metadata
