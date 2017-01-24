SOURCE_BASE=${PWD}
DESTINATION_BASE=${PWD}
mkdir ${DESTINATION_BASE}/data
if [ ! -e ${DESTINATION_BASE}/data/alldata.csv ]
then
    cd ${SOURCE_BASE}/Pipeline_Result
    headerwritten="F"
    for partialmatrix in $(ls *)
    do
      echo "Processing file "$partialmatrix
      
      if [ ${headerwritten} = "F" ]
      then
          cat ${partialmatrix} > ${DESTINATION_BASE}/data/alldata.csv
          headerwritten="T"
      else
          tail -n +2 ${partialmatrix} >> ${DESTINATION_BASE}/data/alldata.csv
      fi
    done
fi
if [ ! -e ${DESTINATION_BASE}/data/aucdata.csv ]
then
    cd ${SOURCE_BASE}/AUC_Result
    headerwritten="F"
    for partialmatrix in $(ls *)
    do
      echo "Processing file "$partialmatrix
      
      if [ ${headerwritten} = "F" ]
      then
          cat ${partialmatrix} > ${DESTINATION_BASE}/data/aucdata.csv
          headerwritten="T"
      else
          tail -n +2 ${partialmatrix} >> ${DESTINATION_BASE}/data/aucdata.csv
      fi
    done
fi
Rscript ${SOURCE_BASE}/ProcessTable.R ${DESTINATION_BASE}/data/alldata.csv ${DESTINATION_BASE}/data/aucdata.csv ${DESTINATION_BASE}/data
rm -f ${DESTINATION_BASE}/data/aucdata.csv