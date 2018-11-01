#!/bin/bash
# Script to publish all of UC Davis Team 37 CTIIP Animal Models data

export LABCAS_SRC=/usr/local/labcas/src/labcas-backend
export PYTHONPATH=$PYTHONPATH:$LABCAS_SRC/common/src/main/python
source /data/local/labcas/labcas_venv/bin/activate
labcas_publish_dataset="python $LABCAS_SRC/common/src/main/python/gov/nasa/jpl/edrn/labcas/client/labcas_dataset_publisher.py"
labcas_publish_collection="python $LABCAS_SRC/common/src/main/python/gov/nasa/jpl/edrn/labcas/client/labcas_collection_publisher.py"

# publish all data at once
# labcas_publish_collection --collection_dir $LABCAS_ARCHIVE/Team_37_CTIIP_Animal_Models

# publish the collection metadata
labcas_publish_collection --collection_dir $LABCAS_ARCHIVE/Team_37_CTIIP_Animal_Models --update_datasets=False --update_files=False

# publish one dataset at a time
datasets=('CTIIP-1.1a.1' 'CTIIP-1.1a.2' 'CTIIP-1.1b' 'CTIIP-1.1c' 'CTIIP-1.1d' 'CTIIP-1.2a' 'CTIIP-1.2b' 'CTIIP-2.0a' 'CTIIP-2.0b' 'CTIIP-2.0c' 'CTIIP-2.1' 'CTIIP-2.2')
for i in "${datasets[@]}"
do
  $labcas_publish_dataset --dataset_dir=$LABCAS_ARCHIVE/Team_37_CTIIP_Animal_Models/$i --collection_name='Team 37 CTIIP Animal Models' --update_collection=False
done
