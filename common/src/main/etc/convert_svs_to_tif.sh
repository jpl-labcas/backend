#!/bin/bash
# script to convert all .svs images in a directory tree to .tif

#directory="/labcas-data/NLST-DATA-FROM-DARTMOUTH/nlst-path/134491"
#directory="/labcas-data/NLST-DATA-FROM-DARTMOUTH/nlst-path/100147"
directory="/labcas-data/NLST-DATA-FROM-DARTMOUTH/nlst-path"
echo $directory
outputdir="/labcas-data/NLST-DATA-FROM-DARTMOUTH/TIFs/"

for f in $(find $directory -name '*.svs'); do
   echo $f
   filepath=`dirname "$f"`
   filename=`basename "$f"`
   pngfilename=${filename//svs/png}
   tiffilename=${filename//svs/tif}
   echo "Converting $filename --> $pngfilename --> $tiffilename"

   if [[ $filename =~ NLSI.* ]]; then
     height="`openslide-show-properties $f | grep "openslide.level\[2\].height" | cut -f 2 -d ':' | cut -f 2 -d "'"`"
     width="`openslide-show-properties $f | grep "openslide.level\[2\].width" | cut -f 2 -d ':' | cut -f 2 -d "'"`"
     echo 'width: '$width' height: '$height
     if [[ -n "$width" && -n "$height" ]]; then
      openslide-write-png $f 0 0 2 $width $height $outputdir$pngfilename
      convert $outputdir$pngfilename $outputdir$tiffilename
      /bin/rm $outputdir$pngfilename
     fi
   else
     height="`openslide-show-properties $f | grep "openslide.level\[3\].height" | cut -f 2 -d ':' | cut -f 2 -d "'"`"
     width="`openslide-show-properties $f | grep "openslide.level\[3\].width" | cut -f 2 -d ':' | cut -f 2 -d "'"`"
     echo 'width: '$width' height: '$height
     if [[ -n "$width" && -n "$height" ]]; then
      openslide-write-png $f 0 0 3 $width $height $outputdir$pngfilename
      convert $outputdir$pngfilename $outputdir$tiffilename
      /bin/rm $outputdir$pngfilename
     fi
   fi
done
