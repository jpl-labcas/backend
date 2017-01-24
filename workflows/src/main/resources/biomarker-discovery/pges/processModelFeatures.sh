#!/bin/bash
#
# Run this file using 'qsub job.sh'
#
# All lines starting with "#$" are SGE qsub commands
#

# Specify which shell to use
#$ -S /bin/bash

# Run on the current working directory
#$ -cwd

# Join standard output and error to a single file
#$ -j y

#$ -l h_rt=48:00:00
#$ -V

# Set Memory allocation
#$ -l mem_free=30g

# Set group
#$ -P pulmseq

# Send an email when the job begins and when it ends running
## $ -m e

# Whom to send the email to
## $ -M anapavel@bu.edu
# Now let's keep track of some information just in case anything goes wrong

PIPELINE_ROUTE=$1
modelFolder=$2
FEATURES_DIR=$3
echo "Processing model ${modelFolder}"
cd ${PIPELINE_ROUTE}
for iterationFolder in $(ls -d */)
do
    cd ${iterationFolder}
    if [ ! -e ${FEATURES_DIR}/${modelFolder}_features.txt ]
    then
        cat ${modelFolder}/feature_names.txt > ${FEATURES_DIR}/${modelFolder}_features.txt
    else
        cat ${modelFolder}/feature_names.txt >> ${FEATURES_DIR}/${modelFolder}_features.txt
    fi
    cd ..
done