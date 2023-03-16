import json
import urllib2
import logging


class SolrClient(object):
    
    def __init__(self, solr_base_url='https://localhost:8984/solr'):
        self._solr_base_url = solr_base_url
        
    def post(self, metadata, solr_core):
        
        json_data_str = self._to_json(metadata)
        self._post_json(json_data_str, solr_core)

    def _to_json(self, data):
        '''
        Converts a Python dictionary or list to json format
        (with unicode encoding).
        '''
        datastr = json.dumps(
            data,
            indent=4,
            sort_keys=True,
            separators=(',', ': '),
            ensure_ascii=False
        )
        return datastr.encode('utf8')
    
    def _post_json(self, json_data_str, solr_core):
        
        req = urllib2.Request(self._get_solr_post_url(solr_core))
        req.add_header('Content-Type', 'application/json')
        logging.debug("Publishing JSON data: %s" % json_data_str)
        response = urllib2.urlopen(req, json_data_str)
        
    def _get_solr_post_url(self, solr_core):
        return "%s/%s/update/json/docs?commit=true" % (self._solr_base_url, solr_core)