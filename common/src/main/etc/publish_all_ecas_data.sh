#!/bin/bash
# shell script to publish all ECAS data to LabCAS
data_dir="/labcas-data/ecas"
archive_dir="/usr/local/labcas/backend/archive"

# create symlinks from $LABCAS_ARCHIVE --> /labcas-data/ecas
for source_dir in $data_dir/* ; do
	
	# extract dataset name = subdir name without the path
    dataset=$(basename "$source_dir")
    
    # symlink target
    target_dir="$archive_dir/$dataset"
    
    if [ ! -d "$target_dir" ]; then
    	    echo "Symlinking $source_dir --> $target_dir"
 		ln -s $source_dir $target_dir
	fi
    
done



