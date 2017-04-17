# Python script that downloads data from the UCDavis SFTP server
# SFTP credentials are retrieved from the environment.
# The script will process all directories under a given root,
# and skip files that have already been downloaded.

# Usage examples: 
# - python ucdavis_sftp_download.py /labcas-data/UCDavis/
# - python ucdavis_sftp_download.py /labcas-data/UCDavis/MMHCC_Image_Archive/
# - python ucdavis_sftp_download.py /labcas-data/UCDavis/MMHCC_Image_Archive/Human_Breast/

import sys
import csv
import os
import pysftp
import re
from glob import glob

HOST = "sftp.mousebiology.org"
USERNAME = os.environ['SFTP_USERNAME']
PASSWORD = os.environ['SFTP_PASSWORD']

# function to download all files in a dataset from the SFTP server
def download_dataset(csv_file_path):
    
    # open connection to the SFTP server
    sftp_server = pysftp.Connection(host=HOST, username=USERNAME, password=PASSWORD)
    
    # get the directory and file listing
    #remote_listing = sftp_server.listdir()
    # prints out the directories and files, line by line
    #for i in remote_listing:
    #    print i
    
    print '\nReading CSV file: %s' % csv_file_path
    # output directory (must include version)
    target_dir = os.path.split(csv_file_path)[0] + "/1"
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
    print 'Downloading files to directory: %s' % target_dir    
    
    # parse CSV file, extract the 'File Location' field
    with open(csv_file_path) as csvfile:
        
        reader = csv.DictReader(csvfile)
        
        for row in reader:
            try:
                src_file_path=row['File Location']
                print 'Detected src_file_path: %s' % src_file_path
                
                # examples of 'File Location':
                # \\ap1314-dsr\Images2\MMHCC Image Archive\Human Breast\MC02-0720.sid.svs
                # \\ap1314-dsr\Images3\MC04\MC04-0006.sid.svs
                if 'images' in src_file_path.lower() or 'images2' in src_file_path.lower():
                    truncated_file_path = re.sub('.*Images\d?','', src_file_path).sub('.*images\d?','', src_file_path)
                    parts = truncated_file_path.split("\\")
                    sftp_path = "/".join(parts)
                    target_file_path = "%s/%s" % (target_dir, parts[-1])
                    if not os.path.exists(target_file_path) or os.path.getsize(target_file_path) == 0:
                        print "\tDownloading: %s to: %s" % (sftp_path, target_file_path)
                        try:
                            sftp_server.get(sftp_path, target_file_path)
                        except Exception as e:
                            print 'Error downloading: %s' % e
                        # cleanup from crashed downloads so bad files don't get published
                        if os.path.exists(target_file_path) and os.path.getsize(target_file_path) == 0:
                            os.remove(target_file_path)
                    else:
                        print 'File %s : %s already exists, skipping' % (sftp_path, target_file_path)
            
            except KeyError as e:
                print 'WARNING: File Location not found for row: %s' % row

    # close the SFTP connection
    sftp_server.close()


# main script
if __name__ == "__main__":

    # loop over all CSV files under root directory
    root_target_dir = sys.argv[1]
    csv_file_paths = [y for x in os.walk(root_target_dir) for y in glob(os.path.join(x[0], '*.csv'))]
    for csv_file_path in csv_file_paths:    
        download_dataset(csv_file_path) 
        