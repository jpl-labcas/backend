# Python script that lists recursively all contents of a remote SFTP site.


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