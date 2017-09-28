# script to publish MD Anderson IPMN data

DATA_DIR=/labcas-data/MD_Anderson_Pancreas_IPMN_images
source /data/local/labcas/labcas_venv/bin/activate
labcas_publish="python $LABCAS_SRC/common/src/main/python/gov/nasa/jpl/edrn/labcas/client/publish_labcas_data.py"

# loop over data subdirectories
for subdir in $DATA_DIR/IPMN_* ; do
    # extract dataset name = subdir name without the path
    dataset=$(basename "$subdir")
    echo "Processing sub-directory: $subdir dataset: $dataset"
    $labcas_publish $LABCAS_METADATA/MD_Anderson_Pancreas_IPMN_images/MD_Anderson_Pancreas_IPMN_images.cfg $DATA_DIR/$dataset/$dataset.cfg --in-place
done
