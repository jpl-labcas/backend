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

def printit(filepath):
    print filepath

# main script
if __name__ == "__main__":

    # open connection to the SFTP server
    sftp_server = pysftp.Connection(host=HOST, username=USERNAME, password=PASSWORD)
    
    # do recursive listing
    sftp_server.walktree('.', printit, printit, printit)
    
    # close the SFTP connection
    sftp_server.close()        