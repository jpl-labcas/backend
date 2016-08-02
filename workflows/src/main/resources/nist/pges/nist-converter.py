''' python script to convert NIST files
    usage: python nist-converter.py [LABCAS_STAGING]/[DatasetId] \
         --DatasetId [DatasetId] \
         --LabNumber [LabNumber] \
         --NormalizationMethod [NormalizationMethod] \
         --RoundNumber [RoundNumber] \
         --LeadPI [LeadPI] \
         --DataCollectionDate [DataCollectionDate] \
         --SampleProcessingProtocols [SampleProcessingProtocols] \
         --InstrumentationTechnologyCode [InstrumentationTechnologyCode] \
         --Manifacturer [Manifacturer] \
         --ModelNumber [ModelNumber] \
         --DataProcessingProtocols [DataProcessingProtocols] \
         --OwnerGroup [OwnerGroup]
'''

import argparse
import logging
from os import listdir, path
from os.path import isfile, join

INPUT_EXTS = ['.txt','.csv']
OUTPUT_EXT = '.nist'
logging.basicConfig(level=logging.DEBUG)

def convert(filepath, filename, metadata_dictionary):
    '''Converts a NIST file from custom to standard format.'''
    
    input_file = join(filepath, filename)
    #output_file = join(filepath, filename.replace(INPUT_EXTS[0], OUTPUT_EXT).replace(INPUT_EXTS[1], OUTPUT_EXT))
    output_file = join(filepath, filename + OUTPUT_EXT)
    logging.info("Converting file: %s --> %s" % (input_file, output_file))
    
    # open output file
    with open(output_file, 'w') as ofile:
        
        # write header lines
        # sort dictionary by key
        for key in sorted(metadata_dictionary):
            if key != 'dir':
                ofile.write('# %s=%s\n' %(key, metadata_dictionary[key]) )
        
        # open input file
        with open(input_file) as ifile:
            # transfer input content
            for line in ifile:
                ofile.write(line)

if __name__ == '__main__':
    
    # parse command line arguments
    parser = argparse.ArgumentParser(description="Python script to convert NIST files into standard format")
    parser.add_argument('dir', type=str, help="Directory containing NIST input files to be converted")
    parser.add_argument('--DatasetId', dest='DatasetId', type=str, help="Dataset Id", default=None)
    parser.add_argument('--LabNumber', dest='LabNumber', type=str, help="Lab Number", default=None)
    parser.add_argument('--NormalizationMethod', dest='NormalizationMethod', type=str, help="Normalization Method", default=None)
    parser.add_argument('--RoundNumber', dest='RoundNumber', type=str, help="Round Number", default=None)
    parser.add_argument('--LeadPI', dest='LeadPi', type=str, help="Lead PI", default=None)
    parser.add_argument('--DataCollectionDate', dest='DataCollectionDate', type=str, help="Data Collection Date", default=None)
    parser.add_argument('--SampleProcessingProtocols', dest='SampleProcessingProtocols', type=str, help="Sample Processing Protocols", default=None)
    parser.add_argument('--InstrumentationTechnologyCode', dest='InstrumentationTechnologyCode', type=str, help="Instrumentation Technology Code", default=None)
    parser.add_argument('--Manufacturer', dest='Manufacturer', type=str, help="Manufacturer", default=None)
    parser.add_argument('--ModelNumber', dest='ModelNumber', type=str, help="Model Number", default=None)
    parser.add_argument('--DataProcessingProtocols', dest='DataProcessingProtocols', type=str, help="Data Processing Protocols", default=None)
    parser.add_argument('--OwnerGroup', dest='OwnerGroup', type=str, help="Owner Group", default=None)
    args_dict = vars( parser.parse_args() )

    for key, value in args_dict.items():
        logging.debug('%s=%s' % (key, value) )
        
    # loop over files in input directory
    datasetId = args_dict['DatasetId']
    for f in listdir(args_dict['dir']):
         if isfile(join(args_dict['dir'], f)):
             file_name, file_extension = path.splitext(f)
             if datasetId in file_name:
                 for input_ext in INPUT_EXTS:
                     if file_extension == input_ext:
                         convert(args_dict['dir'], f, args_dict)    
