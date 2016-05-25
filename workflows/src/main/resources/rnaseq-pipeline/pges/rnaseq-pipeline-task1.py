# python script to execute RNA Sequence Pipeline Task #1
# usage: python rnaseq-pipeline-task1.py --num_threads <num_threads> --gene_transcript_file <gene_transcript_file> --genome_index <genome_index > 
#                                        --data_dir <data_dir> --output_dir <output_dir> --sample_id <sample_id>
# example: python rnaseq-pipeline-task1.py --num_threads 24 --gene_transcript_file genes.gtf --genome_index genome 
#                                          --data_dir /data/EDRN --output_dir thout --sample_id ERR164503

import argparse
import logging
from os import listdir, path
from os.path import isfile, join

logging.basicConfig(level=logging.INFO)

if __name__ == '__main__':
    
    # parse command line arguments
    parser = argparse.ArgumentParser(description="RNA Sequence Pipeline Task #1")
    parser.add_argument('--num_threads', dest='num_threads', type=int, help="Number of THreads", default=1)
    parser.add_argument('--gene_transcript_file', dest='gene_transcript_file', type=str, help="Gene Transcript File", default=None)
    parser.add_argument('--genome_index', dest='genome_index', type=str, help="Genome index", default=None)
    parser.add_argument('--data_dir', dest='data_dir', type=str, help="Data directory", default=None)
    parser.add_argument('--output_dir', dest='output_dir', type=str, help="Output directory", default=None)
    parser.add_argument('--sample_id', dest='sample_id', type=str, help="Sample id", default=None)
    
    args_dict = vars( parser.parse_args() )

    logging.debug('Number of threads=%s' % args_dict['num_threads'])
    logging.debug('Gene transcripit file=%s' % args_dict['gene_transcript_file'])
    logging.debug('Genome index=%s' % args_dict['genome_index'])
    logging.debug('Data Directory=%s' % args_dict['data_dir'])
    logging.debug('Output directory=%s' % args_dict['output_dir'])
    logging.debug('Sample Id=%s' % args_dict['sample_id'])

    # tophat -p 24 -G genes.gtf -o thout genome ERR164503_1.fastq ERR164503_2.fastq
    command = "tophat -p %s -G %s -o %s %s" % (args_dict['num_threads'], args_dict['gene_transcript_file'], 
                                               args_dict['output_dir'], args_dict['genome_index'])

    # loop for .fastq files in sample directory
    input_dir = join(args_dict['data_dir'], args_dict['sample_id'])
    for f in listdir(input_dir):
         if isfile(join(input_dir, f)):
            file_name, file_extension = path.splitext(f)
            if file_extension == '.fastq':
                command += " %s" % f

    # execute command
    logging.info("Executing command: %s" % command)