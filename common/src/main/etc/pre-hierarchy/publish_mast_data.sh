#!/bin/bash

# root data directory
DATA_DIR=/labcas-data/MAST

# metadata directory
METADATA_DIR=/usr/local/labcas/src/labcas-metadata/MAST

# publishing script location
LABCAS_SRC=/usr/local/labcas/src/labcas-backend

# loop over data subdirectories
for subdir in $DATA_DIR/* ; do

    echo "Processing $subdir"

    # extract dataset name = subdir name without the path
    dataset=$(basename "$subdir")
    echo "Datset: $dataset"

    # publish dataset
    script="$LABCAS_SRC/common/src/main/python/gov/nasa/jpl/edrn/labcas/client/publish_labcas_data.py"
    collection_metadata_file="$METADATA_DIR/MAST.cfg"
    dataset_metadata_file="$subdir/$dataset.cfg"
    echo "Executing $script $collection_metadata_file $dataset_metadata_file --in-place"
    python $script $collection_metadata_file $dataset_metadata_file --in-place

    # wait
    sleep 5

done
