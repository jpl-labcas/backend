#!/bin/bash
# note: directories that fail:
# ERR058695, ERR164553, ERR164555, ERR164557, ERR164562, ERR164572, ERR164575, ERR164576, ERR164581,
# ERR164586, ERR164592, ERR164596, ERR164598, 

# root data directory
#DATA_DIR=/usr/local/labcas/backend/staging/RNA_Sequencing
DATA_DIR=/EDRN/RNA_Sequencing

# dataset version
v=1

# metadata directory
METADATA_DIR=/usr/local/labcas/src/labcas-metadata/RNA_Sequencing

# flag to update the collection one time only
first=true

# publishing script location
LABCAS_SRC=/usr/local/labcas/src/labcas-backend

# loop over data subdirectories
for subdir in $DATA_DIR/ERR* ; do
  #if [[ $subdir =~ .*ERR164605 ]] || [[ $subdir =~ .*ERR318894 ]]; then
  #if [[ $subdir =~ .*ERR164605 ]]; then
    # check that gene.counts exists to bypass failed datasets
    if [ -f $subdir/$v/gene.counts ]; then
        echo "Processing $subdir"
        # extract dataset name = subdir name without the path
        dataset=$(basename "$subdir")
        # create metadata file
        cp $METADATA_DIR/TEMPLATE.cfg $METADATA_DIR/$dataset.cfg
        sed -i "s/DATASET_NAME/$dataset/" $METADATA_DIR/$dataset.cfg
        if [ $first == 'true' ]; then
          sed -i "s/UpdateCollection=false/UpdateCollection=true/" $METADATA_DIR/$dataset.cfg
          first=false
        fi
        # publish dataset
        python $LABCAS_SRC/common/src/main/python/gov/nasa/jpl/edrn/labcas/publish_labcas_data.py  $METADATA_DIR/RNA_Sequencing.cfg  $METADATA_DIR/$dataset.cfg --in-place
        # wait
        sleep 5
    fi
  #fi
done
