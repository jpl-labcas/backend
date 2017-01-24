library(pROC)
library(ROCR)
input.all.data.table.route = commandArgs()[6]
input.auc.table.route = commandArgs()[7]
input.data.route = commandArgs()[8]

input.all.data.table = read.csv(input.all.data.table.route)
input.auc.table = read.csv(input.auc.table.route)

auc.mean.performance = c()
rocr.auc.mean.performance=c()
auc.results = c()
rocr.auc.results = c()
for (selected.model.number in unique(input.auc.table[, "Model"])) {
    auc.mean.performance = c(auc.mean.performance, mean(subset(input.auc.table, Model == selected.model.number)[, "AUC"]))
    rocr.auc.mean.performance = c(rocr.auc.mean.performance, mean(subset(input.auc.table, Model == selected.model.number)[, "ROCRAUC"]))
    roc.score.data = subset(input.all.data.table, Model == selected.model.number)[, c("Score", "TrueClass", "Direction")]
    locale.roc.score = roc(as.numeric(as.factor(roc.score.data[, "TrueClass"])), as.numeric(roc.score.data[, "Score"]))$auc
    true.class.vector = as.numeric(as.factor(roc.score.data[, "TrueClass"]))
    if (any(roc.score.data[, "Direction"] == 0)) {
        true.class.vector = 1 - true.class.vector
    }
    rocr.auc.prediction = prediction(as.numeric(roc.score.data[, "Score"]), true.class.vector)
    rocr.auc.performance = performance(rocr.auc.prediction, "auc")@y.values[[1]]
    auc.results = c(auc.results, locale.roc.score)
    rocr.auc.results = c(rocr.auc.results, rocr.auc.performance)
    #locale.header = paste("AUC: ", toString(round(locale.roc.score, digits = 3)), "; ", as.character(specs[selected.model.number, 1]), "; ", as.character(specs[selected.model.number, 2]), "; ", as.character(specs[selected.model.number, 3]), "; ", as.character(specs[selected.model.number, 4]))
}
auc.order.criterion="ROCRAUC"
auc.table.columns = c("Model", "AUCMEAN", "ROCRAUCMEAN", "AUC", "ROCRAUC")
output.auc.means = matrix(nrow = length(auc.mean.performance), ncol = length(auc.table.columns))
colnames(output.auc.means) = auc.table.columns
output.auc.means[, "Model"] = unique(input.auc.table[, "Model"])
output.auc.means[, "AUCMEAN"] = auc.mean.performance
output.auc.means[, "ROCRAUCMEAN"] = rocr.auc.mean.performance
output.auc.means[, "AUC"] = auc.results
output.auc.means[, "ROCRAUC"] = rocr.auc.results
auc.order = order(output.auc.means[, auc.order.criterion], decreasing = T)
output.auc.means = output.auc.means[auc.order,]
setwd(input.data.route)
write.csv(output.auc.means, "aucmeans.csv")
        #auc.performance <- roc.result$auc
