# Python script to reorganize the UCDavis data from S3 to local disk
import csv
import os
import boto
import logging

logging.basicConfig(level=logging.INFO)

CSV_FILE_PATH = '/Users/cinquini/data/MCL_DATA/CTIIP-1.1a.1.csv'
S3_BUCKET_NAME = 'nlstc-data'
S3_BUCKET_PATH = 'UCDavis_Pathology/Team_37_CTIIP_Animal_Models'

db_dict = {}

def read_db():
    
    # read CSV file
    with open(CSV_FILE_PATH) as csvfile:
    
        reader = csv.DictReader(csvfile)
        for row in reader:
            original_file_location = row['File Location']
            parts = original_file_location.split('\\')
            file_name = parts[-1]
            db_dict[file_name] = row.copy()
    
def get_metadata(file_name):
    '''
    Returns metadata for a specific file, if found in the database
    '''
    
    return db_dict.get(file_name, None)


def process_bucket():

    # connect to S3
    conn = boto.connect_s3(profile_name='labcas')
    mybucket = conn.get_bucket(S3_BUCKET_NAME) 
    for obj in mybucket.list(prefix=S3_BUCKET_PATH):
        
        # look for file in database
        (file_path, file_name) = os.path.split(obj.key)
        logging.debug("Querying database for file name=%s" % file_name)
        metadata = get_metadata(file_name)
        if metadata:
            logging.info("FOUND!!! %s" % file_name)


        
        
# main script
if __name__ == "__main__":
    
    read_db()
    
    process_bucket()
        
        