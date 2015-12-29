# python script to convert NIST files
# usage: python nist-convert.py --pi pi --lab lab --instrument instrument --date date

import argparse
import logging
from os import listdir, path
from os.path import isfile, join

INPUT_EXT = '.txt'
OUTPUT_EXT = '.nist'
logging.basicConfig(level=logging.DEBUG)

def convert(filepath, filename, pi=None, instrument=None, lab=None, date=None):
    '''Converts a NIST file from custom to standard format.'''
    
    input_file = join(filepath, filename)
    output_file = join(filepath, filename.replace(INPUT_EXT, OUTPUT_EXT))
    logging.info("Converting file: %s --> %s" % (input_file, output_file))
    
    # open output file
    with open(output_file, 'w') as ofile:
        # write header lines
        ofile.write('# pi=%s\n' % pi)
        ofile.write('# instrument=%s\n' % instrument)
        ofile.write('# lab=%s\n' % lab)
        ofile.write('# date=%s\n' % date)
        
        # open input file
        with open(input_file) as ifile:
            # transfer input content
            for line in ifile:
                ofile.write(line)

if __name__ == '__main__':
    
    # parse command line arguments
    parser = argparse.ArgumentParser(description="Python script to convert NIST files into standard format")
    parser.add_argument('dir', type=str, help="Directory containing NIST input files to be converted")
    parser.add_argument('--pi', dest='pi', type=str, help="Principal Investigator that collected data", default=None)
    parser.add_argument('--instrument', dest='instrument', type=str, help="Instrument used to collect data", default=None)
    parser.add_argument('--lab', dest='lab', type=str, help="Laboratory where data was collected", default=None)
    parser.add_argument('--date', dest='date', type=str, help="Date when data was collected (yyyymmdd)", default=None)
    args_dict = vars( parser.parse_args() )

    logging.debug('Directory=%s' % args_dict['dir'])
    logging.debug('PI=%s' % args_dict['pi'])
    logging.debug('Instrument=%s' % args_dict['instrument'])
    logging.debug('Lab=%s' % args_dict['lab'])
    logging.debug('Date=%s' % args_dict['date'])

    # loop over files in input directory
    for f in listdir(args_dict['dir']):
         if isfile(join(args_dict['dir'], f)):
             file_name, file_extension = path.splitext(f)
             if file_extension == INPUT_EXT:
                 convert(args_dict['dir'], f, 
                         pi=args_dict['pi'], instrument=args_dict['instrument'], lab=args_dict['lab'], date=args_dict['date'])
    
