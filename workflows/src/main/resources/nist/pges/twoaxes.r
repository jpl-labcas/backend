# A graph with 2 y axes.

#postscript("twoaxes.ps")
#pdf("twoaxes.pdf")

    # Load data table.
data_table <- read.table("twoaxes.data", col.names=c("X","Y1","Y2"))

    # prepare params of the three axes, including labels and ticks.
xMin <- 25
xMax <- 50
xMajorTick = 5
xMinorTick = 1

y1Min <- 0
y1Max <- 1800 
y1MajorTick = 500
y1MinorTick = 100

y2Min <-0
y2Max <-100
y2MajorTick = 20 
y2MinorTick = 10


    # axes' major ticks and labels
xMajorLab = seq(xMin, xMax, xMajorTick);
y1MajorLab = seq(y1Min, y1Max, y1MajorTick);
y2MajorLab = seq(y2Min, y2Max, y2MajorTick);

    # minor ticks and no lables;  num ticks == num intervals +1
xMinorLab<- rep("",((xMax - xMin)/xMinorTick) +1) 
y1MinorLab<- rep("",((y1Max - y1Min)/y1MinorTick) +1) 
y2MinorLab<- rep("",((y2Max - y2Min)/y2MinorTick) +1) 

    # axes title strings
xText <- "List Length"
y1Text <- "Total size (megabytes)"
y2Text <- "Space wastage (%)"

    # mark types
y1Mark= 21 #circle
y2Mark= 15 #filled box

    # legend strings 
y1Legend<- "Total size"
y2Legend<- "Space wastage"

    # increase margin to make sure axis titles are displayed.
par(mar = c(4, 4, 2, 4))

plot(data_table$X, data_table$Y1, type="o", axes=FALSE,
     xlim = c(xMin,xMax) , ylim =c(y1Min, y1Max), 
     xlab=xText, ylab=y1Text, pch=y1Mark)
     # cex = 1.5

    # X axis
axis(1, at=seq(xMin,xMax,xMinorTick), labels=xMinorLab, 
        tick=T, tck=-0.01, pos=y1Min)
axis(1, at=xMajorLab,labels=xMajorLab, tick=T, tck=-0.03, pos=y1Min)
#cex.axis = 1.2

    # Y1 axis , las -- label direction.
axis(2, at=seq(y1Min,y1Max,y1MinorTick), labels=y1MinorLab, 
        tick=T, tck=-0.01, pos=xMin)
axis(2, at=y1MajorLab, labels=y1MajorLab, tick=T, tck=-0.03, pos=xMin,
        las=2)

    # make sure next plot is on same graph
par(new = T)

plot(data_table$X, data_table$Y2, type="o", axes=FALSE, 
     xlim = c(xMin,xMax) , ylim =c(y2Min, y2Max), 
     xlab="", ylab="", pch=y2Mark)
mtext(y2Text, 4, 2)

    # Y2 axis
axis(4, at=seq(y2Min,y2Max,y2MinorTick), labels=y2MinorLab, 
        tick=T, tck=-0.01, pos=xMax)
axis(4, at= y2MajorLab, labels=y2MajorLab, tick=T, tck=-0.03, pos=xMax, las=2)

    # Legend, bty="n" -- no frame, 
legend(38, 20, c(y1Legend, y2Legend), bty="n", 
       lty=c(1,1), pch= c(y1Mark, y2Mark))


#dev.off()  # This is only needed if you use pdf/postscript in interactive mode
