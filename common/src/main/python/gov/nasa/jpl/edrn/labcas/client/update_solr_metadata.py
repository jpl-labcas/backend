# Script to update the Solr metadata using input metadata stored in JSON files.
#
# Example invocation: python common/src/main/python/gov/nasa/jpl/edrn/labcas/client/update_solr_metadata.py datasets add input.json
# where input.json is as follows:
#
#{ 
#  "id:Boston_University_Lung_Tumor_Sequencing.FFPE_Lung_Tumor_Sequencing":
#   {
#     "OwnerPrincipal": ["cn=Srivastava National Cancer Institute DCP,ou=groups,o=MCL"]
#   }
#}
#
import sys
import json
from pprint import pprint
from solr_utils import updateSolr

SOLR_URL = 'https://localhost:8984/solr'

if __name__ == '__main__':
    
    solr_core = sys.argv[1]
    updateType = sys.argv[2]
    updateDictFile = sys.argv[3]
    print 'Updating Solr core=%s update type=%s query dictionary file=%s' % (solr_core, updateType, updateDictFile)
    
    with open(updateDictFile) as update_data_file:    
        updateDict = json.load(update_data_file)
    pprint(updateDict)
    
    updateSolr(updateDict, update=updateType, solr_url=SOLR_URL, solr_core=solr_core)