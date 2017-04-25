# Python script to update the Solr metadata for a LabCAS Collection or dataset.
#
# Example usage: python $LABCAS_SRC/common/src/main/python/gov/nasa/jpl/edrn/labcas/update_labcas_metadata.py MMHCC_Image_Archive.cfg
#
# NOTE: metadata keys with empty values will be removed


import sys
import os
import ConfigParser
from gov.nasa.jpl.edrn.labcas.solr_utils import updateSolr

SOLR_URL = 'http://localhost:8983/solr'

if __name__ == '__main__':
    
    # parse configuration files for metadata    
    if len(sys.argv)<2:
        print 'Usage: python update_labcas_metadata.py <path to configuration file(s)>'
        sys.exit(-1)        
        
    config = ConfigParser.ConfigParser()
    # must set following line explicitly to preserve the case of configuration keys
    config.optionxform = str 

    # aggregate metadata from all config files
    for config_file in sys.argv[1:]:
        print "Reading config file: %s" % config_file
        if os.path.isfile(config_file):
            try:
                config.read(config_file)
                for section in config.sections():
                    metadata = {}
                    for key, value in config.items(section):
                        if not value.strip():
                            metadata[key] = [] # empty string
                        elif '|' in value:
                            metadata[key] = [x.strip() for x in value.split('|')] # '|' separated values
                        else:
                            metadata[key] = [value]          # list of one element
                        print '\t%s = %s' % (key, metadata[key])
                        
                    # determine ID of record to update
                    if section == 'Collection':
                        collection_id = metadata['CollectionName'][0].replace(' ','_')
                        solr_core = 'collections'
                        query = 'id:%s' % collection_id
                        
                    elif section == 'Dataset':
                        if metadata.get('DatasetId'):
                            dataset_id = metadata['DatasetId'][0]
                        else:
                            dataset_id = metadata['DatasetName'][0].replace(' ','_')
                        solr_core = 'datasets'
                        query = 'id:*.%s' % dataset_id
                        
                    # send update request to Solr
                    updateDict = { query: metadata }
                    #print updateDict
                    updateSolr(updateDict, update='set', solr_url=SOLR_URL, solr_core=solr_core)
                        
            except Exception as e:
                print e
                sys.exit(-1)

        else:
            print "ERROR configuration file not found: %s" % config_file
            sys.exit(-1)


