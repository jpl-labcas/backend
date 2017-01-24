# 10/29/2016. Author: Ana Brandusa Pavel

library(pROC)
library(ROCR)

library(pROC)
library(ROCR)

 base.route = commandArgs()[6]
 iteration.dir = commandArgs()[7]
 output.file = commandArgs()[8]
auc.output.file = commandArgs()[9]
weighted.voting.file = commandArgs()[10]

 result.route = file.path(base.route, "Pipeline_Result")
  auc.route = file.path(base.route, "AUC_Result")
 source.route = file.path(base.route)
data.route = file.path(base.route, "data")
setwd(source.route)
# source("loadStockPipeline.R")
 message("Source route is ",source.route)
  message("Iteration directory is ",iteration.dir)
    message("Data route is ",data.route)
        message("Result route is ",result.route)
table.columns = c("Iteration", "Model", "ModNames", "Sample", "Score", "Classification", "TrueClass", "Direction")
auc.names = c("Iteration", "Model", "ModNames", "AUC", "ROCRAUC")
# setwd(iteration.dirs)
# iteration.dirs = list.dirs(recursive = F) #
# iteration.dirs = iteration.dirs[!grepl("^(\\.)+$", iteration.dirs)] #
all.data.table = matrix(nrow = 0, ncol = length(table.columns))
auc.data.table = matrix(nrow = 0, ncol = length(auc.names))
weighted.voting.models = read.csv(weighted.voting.file, header=F)[,1]
#for (iteration.dir in iteration.dirs) {
iteration.locale.index = as.numeric(gsub(".*/cv_loop_", "", iteration.dir))
    setwd(iteration.dir)
    model.dirs = list.dirs(recursive = F)
    model.dirs = model.dirs[!grepl("^(\\.)+$", model.dirs)]
    for (model.dir in model.dirs) {
        model.locale.index = as.numeric(gsub("\\./model_", "", model.dir))
        model.name = paste("model", toString(model.locale.index), sep = "_")
        setwd(model.dir)
        message("Entered directory ",model.dir)
        prediction.table = subset(read.table("predictions.txt", dec = ".", header = T, sep = "\t"), !is.na(Score))
        num.preds = nrow(prediction.table)
        weighted.voting = model.name %in% weighted.voting.models
        direction.vector = rep(ifelse(weighted.voting, 1, 0), times = num.preds)
        auc.vector=c()
        if (num.preds > 0) {
            if(num.preds>2){
                class.subset = prediction.table$Response
                if (length(unique(class.subset)) > 1) {
                    score.data = prediction.table[, "Score"]
                    if(any( score.data > 0)){
                        auc.value = roc(as.numeric(as.factor(class.subset)), prediction.table[, "Score"], na.rm = T)$auc
                        response.class.subset = as.numeric(as.factor(class.subset))
                        if (weighted.voting==0) {
                            response.class.subset = 1 - response.class.subset
                        }
                        rocr.auc.prediction = prediction(prediction.table[, "Score"], response.class.subset)
                         rocr.auc.performance=performance(rocr.auc.prediction,"auc")@y.values[[1]]
                        auc.temp.matrix=matrix(nrow=1, ncol=length(auc.names), data=(c(iteration.locale.index, model.locale.index, model.name, auc.value, rocr.auc.performance)))
                        auc.data.table =rbind(auc.data.table , auc.temp.matrix)
                    }
                }
               
            }
            combined.model.prediction.table = cbind(rep(iteration.locale.index, num.preds), rep(model.locale.index, num.preds), rep(model.name, num.preds), prediction.table, direction.vector)
            all.data.table = rbind(all.data.table, combined.model.prediction.table)
            
        }
        setwd("..")
    }
    # setwd("..")
# }
message("Full table is")
message(all.data.table)
colnames(auc.data.table)= auc.names
colnames(all.data.table) = table.columns
auc.data.table = auc.data.table[order(auc.data.table[,"Iteration"], auc.data.table[,"Model"]),]
all.data.table = all.data.table[order(all.data.table$Iteration, all.data.table$Model),]
setwd(auc.route)
write.table(auc.data.table, auc.output.file, sep = ",", row.names = F, quote=F)
setwd(result.route)
write.table(all.data.table, output.file, sep = ",", row.names = F, quote=F)