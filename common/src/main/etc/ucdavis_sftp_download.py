# Python script to parse a CSV file and download the data from the UCDavis SFTP server
# SFTP credentials are retrieved from the environment.

# Usage example: python ucdavis_parse_csv_into_sftp.py /labcas-data/UCDavis/MMHCC_Image_Archive/Human_Breast/Human_Breast.csv

import sys
import csv
import os
import pysftp
import re

HOST = "sftp.mousebiology.org"
USERNAME = os.environ['SFTP_USERNAME']
PASSWORD = os.environ['SFTP_PASSWORD']

# input file
csv_file_path = sys.argv[1]

# output directory (must include version)
target_dir = os.path.split(csv_file_path)[0] + "/1"
if not os.path.exists(target_dir):
    os.makedirs(target_dir)

# open connection to the SFTP server
srv = pysftp.Connection(host=HOST, username=USERNAME, password=PASSWORD)

# get the directory and file listing
#remote_listing = srv.listdir()
# prints out the directories and files, line by line
#for i in remote_listing:
#    print i

# parse CSV file, extract the 'File Location' field
with open(csv_file_path) as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        src_file_path=row['File Location']
        print 'Detected src_file_path: %s' % src_file_path
        
        # examples of 'File Location':
        # \\ap1314-dsr\Images2\MMHCC Image Archive\Human Breast\MC02-0720.sid.svs
        # \\ap1314-dsr\Images3\MC04\MC04-0006.sid.svs
        if 'Images2' in src_file_path:
            truncated_file_path = re.sub('.*Images\d','', src_file_path)
            parts = truncated_file_path.split("\\")
            sftp_path = "/".join(parts)
            target_file_path = "%s/%s" % (target_dir, parts[-1])
            print "\tDownloading: %s to: %s" % (sftp_path, target_file_path)
            srv.get(sftp_path, target_file_path)
            
    
# close the SFTP connection
srv.close()