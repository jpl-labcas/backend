# Python script to publish generic data to the local LabCAS server.
# Metadata must be provided in a python-style configuration file.
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
# Example configuration file:
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
import ConfigParser

from gov.nasa.jpl.edrn.labcas.labcas_client import LabcasClient

if __name__ == '__main__':
    
    # parse configuration file for metadata    
    if len(sys.argv)<2:
        print 'Usage: python publish_labcas_data.py <path to configuration file> [--in-place]'
        sys.exit(-1)        
        
    config_file = sys.argv[1]
    inPlace = False # flag to invoke 'labcas-upload2' workflow to publish the files in-place
    if len(sys.argv)==3 and sys.argv[2].lower()=='--in-place':
        inPlace = True
       
    print 'Using configuration metadata: %s --in-place flag: %s' % (config_file, inPlace)
    config = ConfigParser.ConfigParser()
    # must set following line explicitly to preserve the case of configuration keys
    config.optionxform = str 
    metadata = {}
    try:
        config.read(config_file)
        for section in config.sections():
            for key, value in config.items(section):
                print '\t%s = %s' % (key, value)
                # [Dataset] section: prefix all fields with 'Dataset:'
                if section=='Dataset' and key != 'DatasetName' and key != 'DatasetDescription':
                    metadata['Dataset:%s'%key] = value
                else:
                    metadata[key] = value
    except Exception as e:
        print "ERROR reading metadata configuration:"
        print e
        sys.exit(-1)

    # check mandatory fields
    for key in ['CollectionName', 'CollectionDescription', 'DatasetName', 'OwnerPrincipal', 'Consortium']:
        if not metadata.get(key, None):
            print 'Mandatory metadata field: %s is missing, exiting' % key
            sys.exit(-1)
            
    dataset_name = metadata['DatasetName']
    product_type = metadata['CollectionName'].replace(' ','_') # must match directory name in $LABCAS_STAGING
    
    # submit 'labcas-upload' workflow
    labcasClient = LabcasClient() 
    # print out workflow definition
    #labcasClient.getWorkflowsByEvent("labcas-upload")
    # or equivalently
    if inPlace:
        labcasClient.getWorkflowById("urn:edrn:LabcasUpload2Workflow")
    else:
        labcasClient.getWorkflowById("urn:edrn:LabcasUploadWorkflow")

    # upload dataset staged in directory 'mydatadir'
    labcasClient.uploadCollection(dataset_name, metadata, inPlace=inPlace)

    # query the product types from the XML/RPC File Manager interface
    labcasClient.getProductTypeByName(product_type)
    
    # or equivalently
    labcasClient.getProductTypeById("urn:edrn:%s" % product_type)
    
    # list all products for given product type
    labcasClient.listProducts(product_type)
