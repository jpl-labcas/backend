# Python script to run the wavelet program on all dicom images in a certain directory tree
import os
import glob

rootDir = "/usr/local/labcas/backend/archive/Combined_Imaging_and_Blood_Biomarkers_for_Breast_Cancer_Diagnosis/"
exe = "/usr/local/labcas/labcas_wavelet/Wavelet_C_Code/wavecode"

# first loop over top-level directories
for subDir in glob.glob('%s/E*' % rootDir):
    print("Processing sub-dir: %s" % subDir)
    # recursion into sub-directories
    for dirName, subdirList, fileList in os.walk(subDir):
        print('Processing directory: %s' % dirName)
        for fname in fileList:
            file_name, file_extension = os.path.splitext(fname)
            if file_extension == '.dcm':
               input_file = os.path.join(dirName, fname)
               output_file = input_file.replace(".dcm", ".vff")
               cmd = "%s %s 12 %s 6 1" % (exe, input_file, output_file)
               print('\tExecuting: %s' % cmd)
               os.system(cmd)
