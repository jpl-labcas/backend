# script to list the directory names in the COH data collection

import os
import glob

ROOT_DIR = "/Users/cinquini/data/EDRN_DATA/COH_Data_Collection/KINGSTON1"

#for root, dirs, files in os.walk(ROOT_DIR):
#    if len(dirs) > 0:
#        print(dirs)

def get_subdirs(globexpression):
    subdirs = []
    for path in glob.glob(globexpression):
        if os.path.isdir(path):
            parts = path.split('/')
            subdirs.append(parts[-1])
    return sorted(set(subdirs))
        
subdirs1 = get_subdirs('%s/*' % ROOT_DIR)
print(subdirs1)
subdirs2 = get_subdirs('%s/*/*' % ROOT_DIR)
print(subdirs2)
subdirs3 = get_subdirs('%s/*/*/*' % ROOT_DIR)
print(subdirs3)

max_count = max(len(subdirs1), len(subdirs2), len(subdirs3))

with open('coh.csv','w') as f:
    for i in range(max_count):
        try:
            f.write(subdirs1[i] +",")
        except:
            f.write(",")
        try:
            f.write(subdirs2[i] +",")
        except:
            f.write(",")
        try:
            f.write(subdirs3[i])
        except:
            f.write(",")
        f.write("\n")
    