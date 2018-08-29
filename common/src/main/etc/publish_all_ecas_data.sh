#!/bin/bash
# shell script to publish all ECAS data to LabCAS
data_dir="/labcas-data/ecas"
archive_dir="/usr/local/labcas/backend/archive"

# create symlinks from $LABCAS_ARCHIVE --> /labcas-data/ecas
for source_dir in $data_dir/* ; do
	
	echo $source_dir
	# extract dataset name = subdir name without the path
    dataset=$(basename "$source_dir")
    echo $dataset
    
    target_dir="$archive_dir/$dataset"
    echo $target_dir
    
    if [ ! -d "$target_dir" ]; then
 		ln -s $source_dir $target_dir
	fi
    
done



