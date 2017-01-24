RABBIT_SHINY=${PWD}
mkdir ${RABBIT_SHINY}/modelFeaturesProcessed
rm -f ${RABBIT_SHINY}/modelFeaturesProcessed/*.txt
cd ${RABBIT_SHINY}/modelFeatures
for featureFile in `ls *.txt`
do
    cat ${featureFile} | sort | uniq > ${RABBIT_SHINY}/modelFeaturesProcessed/${featureFile}
done
rm -f ${RABBIT_SHINY}/modelFeatures/*.txt
rm -r ${RABBIT_SHINY}/modelFeatures