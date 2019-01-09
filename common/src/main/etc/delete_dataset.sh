#!/bin/bash
# convenience script to delete a dataset
# usage: ./delete_dataset.sh <dataset_id>
# example: ./delete_dataset.sh mydata

dataset_id=$1
echo 'Deleting dataset_id=$dataset_id'

# delete products (aka files) metadata from Solr
wget "http://localhost:8983/solr/oodt-fm/update?stream.body=<delete><query>DatasetId:${dataset_id}</query></delete>&commit=true"
# cleanup
rm update*

# delete product (aka files) from archive
# will also remove the product type policy files i.e. the product type itself
rm -rf $LABCAS_ARCHIVE/${dataset_id}

# restart LabCAS
#$LABCAS_HOME/stop.sh
#$LABCAS_HOME/start.sh
