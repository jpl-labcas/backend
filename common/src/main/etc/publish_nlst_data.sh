#!/bin/bash

# root data directory
DATA_DIR=/labcas-data/NLST-copy-processed
# dataset version
v=1
# metadata directory
METADATA_DIR=/usr/local/labcas/src/labcas-metadata/NLST
# flag to update the collection one time only
first=true
# publishing script location
LABCAS_SRC=/usr/local/labcas/src/labcas-backend

# loop over data subdirectories
for subdir in $DATA_DIR/* ; do
   # sub-select: start with a digit
  if [[ $subdir =~ [0-9] ]]; then
    echo "Processing $subdir"
    # extract dataset name = subdir name without the path
    dataset=$(basename "$subdir")
    # create metadata file
    cp $METADATA_DIR/TEMPLATE.cfg $METADATA_DIR/$dataset.cfg
    sed -i "s/PATIENT_NUMBER/$dataset/" $METADATA_DIR/$dataset.cfg
    if [ $first == 'true' ]; then
      sed -i "s/UpdateCollection=false/UpdateCollection=true/" $METADATA_DIR/$dataset.cfg
      first=false
    fi
    # publish dataset
    python $LABCAS_SRC/common/src/main/python/gov/nasa/jpl/edrn/labcas/publish_labcas_data.py  $METADATA_DIR/NLST.cfg  $METADATA_DIR/$dataset.cfg --in-place
    # wait
    sleep 5
  fi
done
