LowerLimitsL2R <-function (dat,tol,bw) {
  ### 'dat' is a data frame with columns 'A' and 'DFP'
  ###      'A' contains the average log abundance (or log average abundance?)
  ###      'DFP' contains the differences from predicted (residuals). 
  ###      If multiple data sets are to be considered, 'dat' can be provided
  ###      as a list, with each element containing a data frame following
  ###      the description above.
  ### 'tol' is the tolerance; how far the observed log ratio can
  ###      deviate from the predicted log ratio before the estimate
  ###      is considered useless or misleading
  ### 'bw' is a bandwidth and must be a postive real number.  Larger values
  ###      will produce smoother results.
  bias<-median(dat$DFP, na.rm=TRUE)
  res<-NULL
  if(!"list"%in%class(dat)) dat<-list(dat)
  for(j in 1:length(dat)){
    A<-dat[[j]]$A
    DFP<-dat[[j]]$DFP-bias
    # DFP<-dat[[j]]$DFP
    drop<-which(is.na(A)|is.na(DFP))
    A<-A[-drop]
    DFP<-DFP[-drop]
    t.A<-seq(min(A),max(A),length.out=100)
    A<-A[order(DFP)]
    DFP<-sort(DFP)
    dist<-abs(scale(matrix(A,length(A),length(t.A)),center=t.A,scale=FALSE))
    w<-exp(-dist^2/bw^2)
    w<-scale(w,center=FALSE,scale=colSums(w))
    for(i in 1:length(tol))
      res<-rbind(res,cbind(t.A,colSums(w[which(abs(DFP)<abs(tol[i])),]),tol[i],j))
  }
  colnames(res)<-c("A","Proportion","Tolerance","Data_Set")
  res<-as.data.frame(res)
  for(j in 3:4)  res[,j]<-as.factor(res[,j])
  res
}
