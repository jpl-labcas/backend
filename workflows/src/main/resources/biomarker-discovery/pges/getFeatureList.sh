#!/bin/bash
RABBIT_SHINY=${PWD}
PIPELINE_ROUTE=/restricted/projectnb/pulmseq/Allegro/Bronch_microRNA/edrn243/Analysis/Biomarker_Pipeline/rabbit/rabbit_upd_joe_2016/rabbit/Pipeline_Output_2016_12_16_good
#export PATH=${PIPELINE_ROUTE}R/bin/:$PATH
#export R_LIBS=${PIPELINE_ROUTE}R_libs_3.2.3
mkdir ${RABBIT_SHINY}/modelFeatures
rm -f ${RABBIT_SHINY}/modelFeatures/*.txt
cd ${PIPELINE_ROUTE}
for iterationFolder in $(ls -d */)
do
    cd ${iterationFolder}
    for modelFolder in $(ls -d */)
    do
    echo "Processing model ${modelFolder%%/}"
    if [ ! -e ${RABBIT_SHINY}/modelFeatures/${modelFolder%%/}_features.txt ]
    then
        cat ${modelFolder}/feature_names.txt > ${RABBIT_SHINY}/modelFeatures/${modelFolder%%/}_features.txt
    else
        cat ${modelFolder}/feature_names.txt >> ${RABBIT_SHINY}/modelFeatures/${modelFolder%%/}_features.txt
    fi
    done
    cd ..
done
mkdir ${RABBIT_SHINY}/modelFeaturesProcessed
rm -f ${RABBIT_SHINY}/modelFeaturesProcessed/*.txt
cd ${RABBIT_SHINY}/modelFeatures
for featureFile in `ls *.txt`
do
    cat ${featureFile} | sort | uniq > ${RABBIT_SHINY}/modelFeaturesProcessed/${featureFile}
done
rm -rf ${RABBIT_SHINY}/modelFeatures
