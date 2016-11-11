# R script to run a single iteration of the Stock Pipeline for biomarker discovery

# load the rabbit package and stock pipeline
library(multtest)
data(golub)

library(rabbit)
data(stockPipeline)
colnames(golub) <- paste("Sample", 1:ncol(golub), sep="_")
rownames(golub) <- paste("Gene", 1:nrow(golub), sep="_")

# read in command-line arguments
arguments <- commandArgs(TRUE)

# theoretically we could read in 1) the training set, 2) the output directory, 3) the seed, 4) the iteration number, but really
# we just need the iteration for now
iter <- arguments[1]

# run the stock pipeline
run(stockPipeline, x=golub, y=as.factor(golub.cl), outputdir=getwd(), iter=iter, seed=1234, verbose=TRUE, force=TRUE)
