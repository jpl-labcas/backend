#!/bin/bash
# note: directories that fail:
# ERR058695, ERR164553, ERR164555, ERR164557, ERR164562, ERR164572, ERR164575, ERR164576, ERR164581,
# ERR164586, ERR164592, ERR164596, ERR164598, 

# root data directory
DATA_DIR=/EDRN/RNA_Sequencing
# dataset version
v=1

# loop over subdirectories
for subdir in $DATA_DIR/ERR* ; do
  #if [[ $subdir =~ .*ERR164605 ]]; then
    if [ ! -d $subdir/$v ]; then
        echo "Processing $subdir"
        mkdir -p $subdir/$v
        mv $subdir/*.fastq $subdir/$v
        mv $subdir/*.sra $subdir/$v
        mv $subdir/*.gtf $subdir/$v
        mv $subdir/*.bt2 $subdir/$v
        mv $subdir/*.fa $subdir/$v
        cp $subdir/thout/gene.counts $subdir/$v
    fi
  #fi
done
