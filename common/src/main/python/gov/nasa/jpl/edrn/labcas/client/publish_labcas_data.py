# Python script to publish generic data to the local LabCAS server.
# Metadata must be provided in python-style configuration files (for the collection and dataset).
# Collection and Dataset metadata can be specified in the same file, or in separate files
# 
# a) If the --in-place flag is NOT provided (the default case):
#    - data must be pre-uploaded to the temporary directory $LABCAS_STAGING/<collection_id>/<dataset_id>
#    - the script will invoke the "labcas-upload" workflow to move the files to the $LABCAS_ARCHIVE and publish them.
#
# b) If the --in-place flag IS provided:
#     - data must be pre-uploaded to the permanent directory $LABCAS_ARCHIVE/<collection_id>/<dataset_id>/<dataset_version>
#     - the script will invoke the "labcas-upload2" workflow to publish the files without moving them
#
# Usage: python publish_labcas_data.py <path to configuration file> [--in-place]
#
# Example configuration file(s):
#
#[Collection]
#CollectionName=RNA Sequencing
#CollectionDescription=RNA Sequencing Case Studies
#OwnerPrincipal=uid=amos,dc=edrn,dc=jpl,dc=nasa,dc=gov
#Consortium=MCL
#Discipline=RNA Sequencing
#LeadPI=Chris Amos
#QAState=Public
#Organ=Lung
#Institution=Dartmouth

#[Dataset]
#DatasetName=ERR164503
#DatasetDescription=ERR164503

import sys
import os
import ConfigParser
from xml.sax.saxutils import escape

from labcas_client import LabcasClient

if __name__ == '__main__':
    
    # parse configuration files for metadata    
    if len(sys.argv)<2:
        print 'Usage: python publish_labcas_data.py <path to configuration file>+ [--in-place]'
        sys.exit(-1)        
        
    # loop over command line arguments
    inPlace = False   # flag to publish the files in-place
    debug = False     # flag to print additional information
    config_files = [] # list of configuration files
    for arg in sys.argv[1:]:
        if arg.lower()=='--in-place': # flag
            inPlace = True
        elif arg.lower()=='--debug':  # flag
            debug = True
        else:
            config_files.append(arg)  # config file path
       
    print 'Using configuration files: %s --in-place flag: %s' % (config_files, inPlace)
    config = ConfigParser.ConfigParser()
    # must set following line explicitly to preserve the case of configuration keys
    config.optionxform = str 
    metadata = {}
    # aggregate metadata from all config files
    for config_file in config_files:
        if os.path.isfile(config_file):
            try:
                config.read(config_file)
                for section in config.sections():
                    for key, value in config.items(section):
                        # must escape XML reserved characters: &<>"
                        evalue = escape(value)
                        print '\t%s = %s' % (key, evalue)
                        # [Dataset] section: prefix all fields with 'Dataset:'
                        if section=='Dataset' and key != 'DatasetId' and key != 'DatasetName' and key != 'DatasetDescription':
                            metadata['Dataset:%s'%key] = evalue
                        else:
                            metadata[key] = evalue
            except Exception as e:
                print "ERROR reading metadata configuration:"
                print e
                sys.exit(-1)
        else:
            print "ERROR configuration file not found: %s" % config_file
            sys.exit(-1)

    # check mandatory fields
    for key in ['CollectionName', 'CollectionDescription', 'DatasetName', 'OwnerPrincipal']:
        if not metadata.get(key, None):
            print 'Mandatory metadata field: %s is missing, exiting' % key
            sys.exit(-1)
            
    # use specific DatasetId or generate from DatasetName
    try:
        dataset_id = metadata['DatasetId']
    except KeyError:
        dataset_id = metadata['DatasetName'].replace(' ','_') # must match directory name in $LABCAS_STAGING
    product_type = metadata['CollectionName'].replace(' ','_') # must match directory name in $LABCAS_STAGING
    
    # submit 'labcas-upload' workflow
    labcasClient = LabcasClient() 
    # print out workflow definition
    #labcasClient.getWorkflowsByEvent("labcas-upload")
    # or equivalently
    if debug:
       if inPlace:
          labcasClient.getWorkflowById("urn:edrn:LabcasUpload2Workflow")
       else:
          labcasClient.getWorkflowById("urn:edrn:LabcasUploadWorkflow")

    # upload dataset staged in directory 'mydatadir'
    labcasClient.uploadCollection(dataset_id, metadata, inPlace=inPlace, debug=debug)

    # query the product types from the XML/RPC File Manager interface
    if debug:
       labcasClient.getProductTypeByName(product_type)
    
    # or equivalently
    if debug:
       labcasClient.getProductTypeById("urn:edrn:%s" % product_type)
    
    # list all products for given product type
    if debug:
       labcasClient.listProducts(product_type)
