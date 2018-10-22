# Script that creates dataset metadata for the City Of Hope data collection
# 

import os
import re
import pandas
from datetime import datetime
from utils import write_dataset_metadata

# parameters
data_dir = os.path.join(os.environ['LABCAS_ARCHIVE'], 'City_Of_Hope')
pattern = '.*\/(Du\d+)Breastmri(\d+)\/.*'
csv_filepath = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                            "coh.v2.csv")

columns = ['T2', 'Non-fat supressed T1', 'Pre-contrast', 'Post 1', 'Post 2', 'Post 3', 'Post 4', 'MIP', 'Other', '??']

# read CSV file
df = pandas.read_csv(csv_filepath)
csv_data = {}
for index, row in df.iterrows():
    subdir = row['Subdir'].lower()
    csv_data[subdir] = []
    for column in columns:
        if row[column] == 'x':
            csv_data[subdir].append(column)

# loop over DICOM files in dataset directory tree
print 'Processing directory=%s' % data_dir
for thisdir, subdirs, files in os.walk(data_dir):
    
    # only write out DatasetMetadata.xmlmet for the terminal directories
    if len(subdirs)==0 and len(files)>0:
        print "Processing leaf directory: %s" % thisdir
        (thisdir_path, thisdir_name) = os.path.split(thisdir)
        metadata = {}
        
        matchObj = re.search(pattern, thisdir)
        if matchObj:
            
            patient_id = matchObj.group(1)
            date_acquired = datetime.strptime(matchObj.group(2), '%m%d%Y') 
            
            metadata["PatientId"] = patient_id
            metadata["AcquiredDate"] = date_acquired.strftime('%Y-%m-%dT%H:%M:%SZ')
            
            if thisdir_name.lower() in csv_data:
                metadata['SequenceType'] = "|".join(csv_data[thisdir_name.lower()])
        
            metadata_filepath = os.path.join(thisdir, 'DatasetMetadata.xmlmet')
            write_dataset_metadata(metadata_filepath, metadata)
