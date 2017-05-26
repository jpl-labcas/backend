#!/usr/bin/env Rscript
#setwd("/fullpath/Curated_for_MS_2")
args = commandArgs(TRUE)

if (length(args)<3) {
  stop("Three arguments (Site, Protocol, and Sample) must be supplied (e.g. Lab008, NGS004 and NIST03)\n", call.=FALSE)
}

Site <- args[1]
Protocol <- args[2]
Sample <- args[3]

#######################################
Dashboard6P <- function (Site, Protocol, Sample) {

# # INPUT for line-by-line testing
# setwd("/Users/p_scott_pine/Desktop/Curated_for_MS_2/")
# Site     <- "Lab008"
# Protocol <- "NGS004"
# Sample   <- "NIST03"

# Set Corrections Factors for miRNA abundance
# (default is 1, no correction, unless miRNA spike-ins are used)
Bf <- 1
Lf <- 1
Pf <- 1

# Load libraries
library(reshape2)
library(ggplot2)
library(plyr)
library(gridExtra)
library(grid)
library(ROCR)
library(data.table)

# Load external functions
source("LowerLimit.R")
source("TargetPlot.R")
source("TargetBias.R")

# Import specific files by Sample#
Lab_df <- read.delim(paste(Site,"_",Protocol,"_",Sample,".txt",sep=""))

# Import Sample Key
Sample_Key <- read.delim(paste(Sample,"_Key",".txt",sep=""))

# Import annotation files
miRbase21_mature_wHC <- read.delim("miRbase21_human_mature_wHC.txt")

# Median Normalize if Count Data (skip if PCR)
if(max(Lab_df[2:16], na.rm=TRUE) >= 50){
       Lab_df_colSums <- colSums(Lab_df[2:16], na.rm = TRUE, dims = 1)
       Lab_df_med <- median(Lab_df_colSums)
       Lab_df_factors <- Lab_df_med/Lab_df_colSums
       for(i in 1:length(Lab_df_factors)){Lab_df[i+1]<-Lab_df[i+1]*Lab_df_factors[i]}
}

# Log2 Transform if Count Data (skip if PCR)
Lab_df <- merge(miRbase21_mature_wHC, Lab_df, by="Mature_Name", all.x=TRUE)
ifelse(max(Lab_df[8:22], na.rm=TRUE) >= 50,
       Lab_df <- cbind(Lab_df[1:7], log2(Lab_df[8:22])),
       Lab_df <- cbind(Lab_df[1:7], (-1)*Lab_df[8:22]))
Lab_df$Site_Platform <- Protocol

# Reshape Tall
Lab_df_tall <- melt(Lab_df)

# Annotate
Lab_df_tall <- rename(Lab_df_tall, c(variable="Sample",value="Log2signal"))
Lab_df_tall <- merge(Lab_df_tall, Sample_Key, by="Sample")

### Average Replicates Function ###
AvgTissueReps <- function(x) {  
  Input <- x
  Tissue <- subset(Lab_df_tall, Tissue==Input)  
  Rep1 <- subset(Tissue, Replicate=="Rep1")
  Rep1 <- rename(Rep1, c(Log2signal="Rep1Log2signal"))
  Rep2 <- subset(Tissue, Replicate=="Rep2")
  Rep2 <- rename(Rep2, c(Log2signal="Rep2Log2signal"))
  Rep3 <- subset(Tissue, Replicate=="Rep3")
  Rep3 <- rename(Rep3, c(Log2signal="Rep3Log2signal"))  
  Wide <- cbind(Rep1[2:9],
                    Rep1[11],
                    Rep1$Rep1Log2signal,
                    Rep2$Rep2Log2signal,
                    Rep3$Rep3Log2signal)
  Mean   <- apply(Wide[10:12],1,mean, na.rm=TRUE)
  SD     <- apply(Wide[10:12],1,sd, na.rm=TRUE)
  MeanSD <- cbind(Wide,Mean,SD)
  return(MeanSD)
}
###################################

### Analyze NEAT tissues: Brain, Liver, and Placenta
Brain_Avg    <- AvgTissueReps("Brain")
Liver_Avg    <- AvgTissueReps("Liver")
Placenta_Avg <- AvgTissueReps("Placenta")

BLP_Avg <- rbind(Brain_Avg,
                 Liver_Avg,
                 Placenta_Avg)

# Calculate T1vT2 Ratios
M  <-      Brain_Avg$Mean - Liver_Avg$Mean
SD <- sqrt(Brain_Avg$SD^2 + Liver_Avg$SD^2)
A  <-     (Brain_Avg$Mean + Liver_Avg$Mean)/2
BvL_R <- cbind(Brain_Avg[1:8],M,A,SD)
BvL_R$T1vT2 <- "BvL"

M  <-      Placenta_Avg$Mean - Brain_Avg$Mean
SD <- sqrt(Placenta_Avg$SD^2 + Brain_Avg$SD^2)
A  <-     (Placenta_Avg$Mean + Brain_Avg$Mean)/2
PvB_R <- cbind(Brain_Avg[1:8],M,A,SD)
PvB_R$T1vT2 <- "PvB"

M  <-      Placenta_Avg$Mean - Liver_Avg$Mean
SD <- sqrt(Placenta_Avg$SD^2 + Liver_Avg$SD^2)
A  <-     (Placenta_Avg$Mean + Liver_Avg$Mean)/2
PvL_R <- cbind(Brain_Avg[1:8],M,A,SD)
PvL_R$T1vT2 <- "PvL"

T1vT2_ALL <- rbind(BvL_R, PvB_R, PvL_R)

# Analyze Mix1 and Mix2
Mix1_Avg    <- AvgTissueReps("Mix1")
Mix2_Avg    <- AvgTissueReps("Mix2")

M1vM2_Avg <- rbind(Mix1_Avg, Mix2_Avg)

# In Silico Modeling of Mixtures
LinB <- 2^Brain_Avg$Mean
LinL <- 2^Liver_Avg$Mean
LinP <- 2^Placenta_Avg$Mean

LinLP <- cbind(LinL, LinP)
LinBP <- cbind(LinB, LinP)
LinBL <- cbind(LinB, LinL)

LinLP_max <- apply(LinLP[,1:2],1,max,na.rm=TRUE)
LinBP_max <- apply(LinBP[,1:2],1,max,na.rm=TRUE)
LinBL_max <- apply(LinBL[,1:2],1,max,na.rm=TRUE)

TSI_B <- LinB/LinLP_max
TSI_L <- LinL/LinBP_max
TSI_P <- LinP/LinBL_max

# Identify 10X Tissue-Selective miRNAs
TSI_Class <- ifelse(TSI_B >=10 , "10X_Brain",
                    ifelse(TSI_L >=10, "1_to_1", # 10X_Liver incorporated into 1_to_1 TSI_Class
                           ifelse(TSI_P >=10, "10X_Placenta",
                                  ifelse(abs(log2(LinB/LinP/LinL)) ==0.0, "NS",
                                         ifelse(abs(log2(LinB/LinP)) <=0.075, "1_to_1", "NS")))))

TSI <- cbind(TSI_B, TSI_L, TSI_P, TSI_Class)

ModM1 <- log2(((LinB*0.25)/Bf)+((LinL*0.25)/Lf)+((LinP*0.5)/Pf))
ModM2 <- log2(((LinB*0.5)/Bf)+((LinL*0.25)/Lf)+((LinP*0.25)/Pf))

# Calculate Predicted Ratios
P <- ModM1 - ModM2

# Calculate Observed Ratios
M  <-      Mix1_Avg$Mean - Mix2_Avg$Mean
SD <- sqrt(Mix1_Avg$SD^2 + Mix2_Avg$SD^2)
A  <-     (Mix1_Avg$Mean + Mix2_Avg$Mean)/2

# Calculate Difference From Predicted (DFP) Ratios
DFP <- M-P

# Make Dataframe
M1vM2_R <- cbind(Mix1_Avg[1:8],M,A,SD,P,DFP)
M1vM2_R$T1vT2 <- "M1vM2"

# Fix Infinities
M1vM2_R[M1vM2_R=="-Inf"]<-NaN
M1vM2_R[M1vM2_R=="Inf"]<-NaN

# Dataframe for Bland-Altman Plots
A_ModM1M2 <- (ModM1 + ModM2)/2
M_ModM1M2 <- ModM1 - ModM2

Mix1_v_Mix2 <- cbind(M1vM2_R, A_ModM1M2, M_ModM1M2, TSI_Class)

Mix1_v_Mix2_A   <- subset(Mix1_v_Mix2, TSI_Class=="1_to_1")
Mix1_v_Mix2_A$labels <- "0"
Mix1_v_Mix2_B   <- subset(Mix1_v_Mix2, TSI_Class=="10X_Brain")
Mix1_v_Mix2_B$labels <- "1"
Mix1_v_Mix2_C   <- subset(Mix1_v_Mix2, TSI_Class=="10X_Placenta")
Mix1_v_Mix2_C$labels <- "1"

Mix1_v_Mix2_ABC <- rbind(Mix1_v_Mix2_A, Mix1_v_Mix2_B, Mix1_v_Mix2_C)

# BAplot of PREDICTED Mixtures (w/ non-selective)
PredictedPlot <- ggplot(Mix1_v_Mix2, aes(A_ModM1M2, M_ModM1M2, fill=TSI_Class, shape=TSI_Class, alpha=TSI_Class))  +
  geom_point(size=2) +
  geom_smooth(data=Mix1_v_Mix2_ABC , method="loess", span=1.0, size=0.5, alpha=0.3, color="black") +
  scale_fill_manual(values = c("yellow","blue", "red", "black")) +
  scale_shape_manual(values = c(21,21,21,1)) +
  scale_alpha_manual(values = c(1.0,1.0,1.0,0.3)) +
  geom_hline(aes(yintercept = 1), color="red", size=1.0, linetype=2) +
  geom_hline(aes(yintercept = 0), color="orange", size=1.0, linetype=2) +
  geom_hline(aes(yintercept = -1), color="blue", size=1.0, linetype=2) +
  ylim(-2.5,2.5) +
  # xlim(0,25) +
  theme(legend.position = "none") +
  labs(x = "Average Log2Signal",
       y = "Predicted Log2ratio",
       title = " ")

# BAplot of OBSERVED Mixtures (w/ non-selective)
ObservedPlot <- ggplot(Mix1_v_Mix2, aes(A, M, fill=TSI_Class, shape=TSI_Class, alpha=TSI_Class))  +
  geom_point(size=2) +
  geom_smooth(data=Mix1_v_Mix2_ABC , method="loess", span=1.0, size=0.5, alpha=0.3, color="black") +
  scale_fill_manual(values = c("yellow","blue", "red", "black")) +
  scale_shape_manual(values = c(21,21,21,1)) +
  scale_alpha_manual(values = c(1.0,1.0,1.0,0.3)) +
  geom_hline(aes(yintercept = 1), color="red", size=1.0, linetype=2) +
  geom_hline(aes(yintercept = 0), color="orange", size=1.0, linetype=2) +
  geom_hline(aes(yintercept = -1), color="blue", size=1.0, linetype=2) +
  ylim(-2.5,2.5) +
  # xlim(0,25) +
  theme(legend.position = "none") +
  labs(x = "Average Log2Signal",
       y = "Observed Log2ratio",
       title = " ")

# Tolerance calculations for DFPrPlot
bias <- median(Mix1_v_Mix2$DFP, na.rm=TRUE)
iqr  <- (IQR(Mix1_v_Mix2$DFP, na.rm=TRUE))

# defaults used for MS (allow user input in later version)
bw   <- 2            
tol  <- 0.585        # log2 of 1.5X fold-change - up or down  (allow user input in later version)
prop <- 0.95         

# pass dataframe to LowerLimit function
dat <- Mix1_v_Mix2

res1 <- LowerLimitsL2R(dat,tol,bw)
LL <- as.numeric(summarise(res1, thresh=min(A[Proportion>prop])))
UL <- max((Mix1_v_Mix2$A + Mix1_v_Mix2$A_ModM1M2)/2, na.rm = TRUE)

# BAplot of DFP
DFPrPlot <- ggplot(Mix1_v_Mix2, aes((A+A_ModM1M2)/2, M-M_ModM1M2, fill=TSI_Class, shape=TSI_Class, alpha=TSI_Class))  +
  geom_rect(aes(xmin=-Inf, xmax=LL, ymin=-Inf, ymax=Inf), fill="grey") +
  geom_rect(aes(xmin=UL, xmax=Inf, ymin=-Inf, ymax=Inf), fill="grey") +
  # geom_vline(aes(xintercept = LL), color="black", size=.75, linetype=2) +
  # geom_vline(aes(xintercept = UL), color="black", size=.75, linetype=2) +
  geom_hline(aes(yintercept = 0), color="white", size=1, linetype=1, alpha=0.5) +
  geom_hline(aes(yintercept = 1), color="white", size=.25, linetype=1, alpha=0.5) +
  geom_hline(aes(yintercept = 2), color="white", size=.25, linetype=1, alpha=0.5) +
  geom_hline(aes(yintercept = 3), color="white", size=.25, linetype=1, alpha=0.5) +
  geom_hline(aes(yintercept = -1), color="white", size=.25, linetype=1, alpha=0.5) +
  geom_hline(aes(yintercept = -2), color="white", size=.25, linetype=1, alpha=0.5) +
  geom_hline(aes(yintercept = -3), color="white", size=.25, linetype=1, alpha=0.5) +
  geom_point(size=2) +
  scale_fill_manual(values = c("yellow","blue", "red", "black")) +
  scale_shape_manual(values = c(21,21,21,1)) +
  scale_alpha_manual(values = c(1.0,1.0,1.0,0.3)) +
  geom_hline(aes(yintercept = bias), color="black", size=.75, linetype=1) +
  geom_hline(aes(yintercept = iqr+bias), color="black", size=.75, linetype=2) +
  geom_hline(aes(yintercept = -iqr+bias), color="black", size=.75, linetype=2) +
  ylim(-3,3) +
  # scale_x_continuous(limits=c(0,25),breaks=c(0,5,10,15,20,25)) +
  theme(legend.position = "none") +
  labs(x = "Average Log2Signal",
       y = "Observed - Predicted",
       title = " ")

# TargetPlot
Signal <- 2^Lab_df_tall$Log2signal
Lab_df_tall_wSignal <- cbind(Lab_df_tall, Signal)
Lab_df_tall_recast  <- dcast(Lab_df_tall_wSignal, Site_Platform + Mature_Name + Replicate ~ Tissue, value.var="Signal")

TargetPlot<-makeTargetPlot(Protocol, indf = Lab_df_tall_recast, correction=c(Bf,Lf,Pf))

# ROCplot
M1M2_Wide <-cbind(Mix1_Avg[1:12], Mix2_Avg[10:12])
M1M2_Wide_Lab <- subset(M1M2_Wide, Site_Platform==Protocol)

# Require minimum of 2 non-NA
M1M2_Wide_Lab <- M1M2_Wide_Lab[rowSums(is.na(M1M2_Wide_Lab[10:12])) < (length(M1M2_Wide_Lab[10:12])-1),]
M1M2_Wide_Lab <- M1M2_Wide_Lab[rowSums(is.na(M1M2_Wide_Lab[13:15])) < (length(M1M2_Wide_Lab[13:15])-1),]

# T-Test
M1M2_Wide_Lab$Pval <- sapply(1:nrow(M1M2_Wide_Lab), function(i) t.test(M1M2_Wide_Lab[i,10:12], M1M2_Wide_Lab[i,13:15])$p.value)

M1M2_Wide_Lab_annot <- merge(M1M2_Wide_Lab, Mix1_v_Mix2_ABC, by="Mature_Name", all.y=TRUE)
M1M2_Wide_Lab_annot$predictions <- 1 - M1M2_Wide_Lab_annot$Pval

# ALL Tissue-selective
pred_ALL <- prediction(M1M2_Wide_Lab_annot$predictions, M1M2_Wide_Lab_annot$labels)
perf_ROC_ALL <- performance(pred_ALL,"tpr","fpr")

ALL_x <- data.frame(perf_ROC_ALL@x.values)
FPR <-ALL_x[,1]
ALL_y <- data.frame(perf_ROC_ALL@y.values)
TPR <-ALL_y[,1]
ALL_xy <- data.frame(cbind(TPR,FPR))

perf_AUC_ALL <- performance(pred_ALL,"auc")
AUC <- unlist(slot(perf_AUC_ALL, "y.values"))

# Above LL test
above_LL_test <- subset(M1M2_Wide_Lab_annot, ((A+A_ModM1M2)/2) > LL)
TP = nrow(subset(above_LL_test, labels==1))
TN = nrow(subset(above_LL_test, labels==0))
ifelse(TP*TN==0, LLtest <- "LLpoor", LLtest <- "LLgood")

if(TP*TN==0){
  AUC_R <- NA
  ROCplot <- ggplot() +
    geom_line(data=ALL_xy, aes(x=FPR, y=TPR), size=1, color="black", linetype=1) +
    labs(title = " ")
}else{
  above_LL <- subset(M1M2_Wide_Lab_annot, ((A+A_ModM1M2)/2) > LL)
  pred_LL <- prediction(above_LL$predictions, above_LL$labels)
  perf_ROC_LL <- performance(pred_LL,"tpr","fpr")
  LL_x <- data.frame(perf_ROC_LL@x.values)
  FPR_2 <-LL_x[,1]
  LL_y <- data.frame(perf_ROC_LL@y.values)
  TPR_2 <-LL_y[,1]
  LL_xy <- data.frame(cbind(TPR_2,FPR_2))
  perf_AUC_LL <- performance(pred_LL,"auc")
  AUC_R <- unlist(slot(perf_AUC_LL, "y.values"))
  ROCplot <- ggplot() +
    geom_line(data=ALL_xy, aes(x=FPR, y=TPR), size=1, color="black", linetype=1) +
    geom_line(data=LL_xy, aes(x=FPR_2, y=TPR_2), size=1, color="black", linetype=2) +
    labs(title = " ")
}

### BoxPlots by TSI Class
DFPrBoxPlot <- ggplot(na.omit(Mix1_v_Mix2), aes(TSI_Class, M-M_ModM1M2, fill=TSI_Class))  +
  geom_hline(aes(yintercept = 0), color="white", size=1, linetype=1) +
  scale_fill_manual(values = c("yellow","blue", "red", "darkgrey")) +
  geom_boxplot(outlier.shape=95, outlier.size=5, outlier.colour = "black") +
  geom_hline(aes(yintercept = bias), color="black", size=.75, linetype=1) +
  geom_hline(aes(yintercept = iqr+bias), color="black", size=.75, linetype=2) +
  geom_hline(aes(yintercept = -iqr+bias), color="black", size=.75, linetype=2) +
  scale_x_discrete(breaks=c("1_to_1","10X_Brain","10X_Placenta","NS"),
                   labels=c("1-to-1","Brain","Placenta","NS")) +
  ylim(-3,3) +
  theme(legend.position = "none") +
  labs(
    y = "Observed - Predicted",
    x = "Tissue-Selective Class",
    title = " ")

### Calculate All Ratio Metrics
# Name TSI Class
TSI_Class <- c("1-to-1*", "Brain", "Placenta", "NS", "All")

# Count TSI Classes
Liver    <- nrow(subset(data.frame(Mix1_v_Mix2), TSI_Class=="1_to_1"))
Brain    <- nrow(subset(data.frame(Mix1_v_Mix2), TSI_Class=="10X_Brain"))
Placenta <- nrow(subset(data.frame(Mix1_v_Mix2), TSI_Class=="10X_Placenta"))
NS       <- nrow(subset(data.frame(Mix1_v_Mix2), TSI_Class=="NS"))
All      <- Liver+Brain+Placenta+NS

Det_Count <- c(Liver,Brain,Placenta,NS,All)

# Calculate Ratio Bias
Liver    <- round(median((subset(data.frame(Mix1_v_Mix2), TSI_Class=="1_to_1"))$DFP, na.rm=TRUE),4)
Brain    <- round(median((subset(data.frame(Mix1_v_Mix2), TSI_Class=="10X_Brain"))$DFP, na.rm=TRUE),4)
Placenta <- round(median((subset(data.frame(Mix1_v_Mix2), TSI_Class=="10X_Placenta"))$DFP, na.rm=TRUE),4)
NS       <- round(median((subset(data.frame(Mix1_v_Mix2), TSI_Class=="NS"))$DFP, na.rm=TRUE),4)
All      <- round(median(Mix1_v_Mix2$DFP, na.rm=TRUE),4)

Med_DFPr <- c(Liver,Brain,Placenta,NS,All)

# Calculate Ratio Dispersion
Liver    <- round(IQR((subset(data.frame(Mix1_v_Mix2), TSI_Class=="1_to_1"))$DFP, na.rm=TRUE, type=7),3)
Brain    <- round(IQR((subset(data.frame(Mix1_v_Mix2), TSI_Class=="10X_Brain"))$DFP, na.rm=TRUE, type=7),3)
Placenta <- round(IQR((subset(data.frame(Mix1_v_Mix2), TSI_Class=="10X_Placenta"))$DFP, na.rm=TRUE, type=7),3)
NS       <- round(IQR((subset(data.frame(Mix1_v_Mix2), TSI_Class=="NS"))$DFP, na.rm=TRUE, type=7),3)
All      <- round(IQR(Mix1_v_Mix2$DFP, na.rm=TRUE, type=7),3)

IQR_DFPr <- c(Liver,Brain,Placenta,NS,All)

# Add Deviation from Target Vectors
Vectors<-getTargetVectors(Protocol, indf = Lab_df_tall_recast, correction=c(Bf,Lf,Pf))
Vectors<-apply(Vectors,2,as.character)
Vectors[is.na(Vectors)]<-""
Vectors<-as.data.frame(Vectors)

# Make Output Table
MetricsGrid0 <- paste("Site =", Site,
                      "  Protocol =", Protocol,
                      "  Sample =", Sample)

MetricsGrid1 <- cbind(TSI_Class, Det_Count, Med_DFPr, IQR_DFPr, Vectors)
colnames(MetricsGrid1) <- c("Class",
                            "Detected",
                            "Median",
                            "IQR",
                            "Component",
                            "Mix1",
                            "Mix2",
                            "Bias")

MetricsGrid2 <- cbind(round(LL, 2), round(UL, 2), round(UL-LL, 2), round(AUC,3), round(AUC_R,3))
colnames(MetricsGrid2) <- c("LowerLimit","Maximum","Range ","  AUC (All)  ","  AUC (Range)  ")

tt_min <- ttheme_minimal(core = list(padding=unit(c(1, 1), "mm")),
                         base_size = 10)

MetricsTable <- arrangeGrob(
  tableGrob(MetricsGrid0, theme=tt_min, rows=NULL),
  tableGrob(MetricsGrid1, theme=tt_min, rows=NULL),
  tableGrob(MetricsGrid2, theme=tt_min, rows=NULL),
  nrow=3)

# Final Multi-Panel Figure for PDF
g0 <- arrangeGrob(MetricsTable, ncol=1, nrow=1, widths=c(3))
g1 <- arrangeGrob(PredictedPlot, TargetPlot, ncol=2, nrow=1, widths=c(2, 1))
g2 <- arrangeGrob(ObservedPlot, ROCplot,     ncol=2, nrow=1, widths=c(2, 1))
g3 <- arrangeGrob(DFPrPlot, DFPrBoxPlot,     ncol=2, nrow=1, widths=c(2, 1))

g <- grid.arrange(g0, g1, g2, g3, ncol=1, nrow=4)

ggsave(file=paste(Site,"_",Protocol,"_",Sample,".pdf", sep=""), g, width = 7.5, height = 10)

}
###########################
Dashboard6P(Site, Protocol, Sample)
