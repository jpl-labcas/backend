#!/bin/sh
# Script to publish all labcas collections, datasets, files
alias labcas_publish='python $LABCAS_SRC/common/src/main/python/gov/nasa/jpl/edrn/labcas/publish_labcas_data.py'

# UC Davis 1.0_Athena-IHC4_with_Swedish_Breast_Cancer_Study
cd /usr/local/labcas/src/labcas-metadata/1.0_Athena-IHC4_with_Swedish_Breast_Cancer_Study
labcas_publish 1.0_Athena-IHC4_with_Swedish_Breast_Cancer_Study.cfg 1.0_Test_Cases.cfg --in-place
labcas_publish 1.0_Athena-IHC4_with_Swedish_Breast_Cancer_Study.cfg ER-EP1_v._SP1_v._CM2.cfg --in-place
labcas_publish 1.0_Athena-IHC4_with_Swedish_Breast_Cancer_Study.cfg ST03.1_Stockholm_Tamoxifen_Trial.cfg --in-place
labcas_publish 1.0_Athena-IHC4_with_Swedish_Breast_Cancer_Study.cfg ST03.2_Stockholm_Tamoxifen_Trial.cfg --in-place

# UC Davis Team_37_CTIIP_Animal_Models
cd /usr/local/labcas/src/labcas-metadata/Team_37_CTIIP_Animal_Models
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-1.1a.1.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-1.1a.2.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-1.1b.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-1.1c.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-1.1d.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-1.2a.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-1.2b.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-2.0a.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-2.0b.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-2.0c.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-2.1.cfg --in-place
labcas_publish Team_37_CTIIP_Animal_Models.cfg CTIIP-2.2.cfg --in-place

# UC Davis MMHCC_Image_Archive
cd /usr/local/labcas/src/labcas-metadata/MMHCC_Image_Archive
labcas_publish MMHCC_Image_Archive.cfg Human_Breast.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Human_Derm.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Human_GI.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Human_Lung.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Human_Normal_Histology.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Human_Prostate.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_GI_Pathology_from_JAX_workshop.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Hematopathology.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Image_Archive.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Lung.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Mammary.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Neuropathology.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Normal.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Ovary.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Prostate.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg MMHCC_Skin.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Mouse_CNS.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Mouse_Derm.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Mouse_GI.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Mouse_Hematopoietic_System.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Mouse_Lung.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Mouse_Mammary.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Mouse_Normal_Histology.cfg --in-place
labcas_publish MMHCC_Image_Archive.cfg Mouse_Prostate.cfg --in-place

# MD_Anderson_Pancreas_IPMN_images
cd /usr/local/labcas/src/labcas-metadata/MD_Anderson_Pancreas_IPMN_images
labcas_publish IPMN_P1-06_H03.cfg

# CBIS-DDSM
cd /usr/local/labcas/src/labcas-metadata/CBIS-DDSM
labcas_publish CBIS-DDSM.cfg Calc-Training_Full_Mammogram_Images.cfg --in-place
labcas_publish CBIS-DDSM.cfg Mass-Training_Full_Mammogram_Images.cfg --in-place
labcas_publish CBIS-DDSM.cfg Mass-Training_ROI_and_Cropped_Images.cfg --in-place

# University_of_Colorado_Lung_Image
#cd /usr/local/labcas/src/labcas-metadata/University_of_Colorado_Lung_Image
#labcas_publish University_of_Colorado_Lung_Image.cfg UCHSC_1467.cfg --in-place
#labcas_publish University_of_Colorado_Lung_Image.cfg UCHSC_8798.cfg --in-place

# RNA Sequencing
cd /usr/local/labcas/src/labcas-backend/common/src/main/etc
./publish_rnaseq_data.sh
