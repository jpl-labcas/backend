# python script to execute RNA Sequence Pipeline Task #1
# usage: python rnaseq-pipeline-task1.py --num_threads <num_threads> --gene_transcript_file <gene_transcript_file> --genome_index <genome_index > 
#                                        --data_dir <data_dir> --output_dir <output_dir> --sample_id <sample_id>
# example: python rnaseq-pipeline-task1.py --num_threads 24 --gene_transcript_file genes.gtf --genome_index genome 
#                                          --data_dir /data/EDRN --output_dir thout --sample_id ERR164503

import argparse
import os

INPUT_FILE_EXTENSIONS = ['.sra','.fastq','.gtf','.fa','.bt2']

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

    print 'Number of threads=%s' % args_dict['num_threads']
    print'Gene transcripit file=%s' % args_dict['gene_transcript_file']
    print'Genome index=%s' % args_dict['genome_index']
    print'Data Directory=%s' % args_dict['data_dir']
    print'Output directory=%s' % args_dict['output_dir']
    print'Sample Id=%s' % args_dict['sample_id']

    # tophat -p 24 -G genes.gtf -o thout genome ERR164503_1.fastq ERR164503_2.fastq
    command = "tophat -p %s -G %s -o %s %s" % (args_dict['num_threads'], 
                                               args_dict['gene_transcript_file'], 
                                               args_dict['output_dir'], 
                                               args_dict['genome_index'])

    # symlink or copy input data into working directory
    # look for .fastq files in sample directory
    input_dir = os.path.join(args_dict['data_dir'], args_dict['sample_id'])
    for f in os.listdir(input_dir):
        input_file = os.path.join(input_dir, f)
        if os.path.isfile(input_file):
            file_name, file_extension = os.path.splitext(f)
            if file_extension == '.fastq':
                command += " %s" % f
            if file_extension in INPUT_FILE_EXTENSIONS:
                if not os.path.exists(f):
                    if file_extension == '.fastq':
                        # must copy files that we want to publish to the same directory where the metadata file will be created
                        # otherwise the FM will not be able to publish the file
                        os.system('cp '+ input_file + ' ' + f)
                    else:
                        # symlink other files
                        os.symlink(input_file, f)

    # execute command
    print "Executing command: %s" % command
    os.system( command )