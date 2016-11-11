#!/bin/bash
#
# Shell script to execute the R Stock Pipeline script for a given iteration
# 
# Example invocation: run_stock_pipeline_iter.sh $i

# print some debugging information at start time
echo "=========================================================="
echo "Starting on       : $(date)"
echo "Running on node   : $(hostname)"
echo "Current directory : $(pwd)"
echo "Iteration number : $1"
echo "=========================================================="
echo ""

# execute R script located in the same directory
R --no-save < ./run_stock_pipeline_iter.R --args $1

# print some debugging information at stop time
echo ""
echo "=========================================================="
echo "Iteration number: $1 finished on       : $(date)"
echo "=========================================================="
