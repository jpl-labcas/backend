#!/bin/bash
# script to convert all .svs images in a directory tree to .tif

#directory="/labcas-data/NLST-DATA-FROM-DARTMOUTH/nlst-path/134491"
directory="/labcas-data/NLST-DATA-FROM-DARTMOUTH/nlst-path"
echo $directory

for f in $(find $directory -name '*.svs'); do
   echo $f
   height="`openslide-show-properties $f | grep "openslide.level\[3\].height" | cut -f 2 -d ':' | cut -f 2 -d "'"`"
   width="`openslide-show-properties $f | grep "openslide.level\[3\].width" | cut -f 2 -d ':' | cut -f 2 -d "'"`"
   echo 'width: '$width' height: '$height
   if [[ -n "$width" && -n "$height" ]]; then
      filepath=`dirname "$f"`
      filename=`basename "$f"`
      pngfilename=${filename//svs/png}
      tiffilename=${filename//svs/tif}
      echo "Converting $filename --> $pngfilename --> $tiffilename"
      openslide-write-png $f 0 0 3 $width $height $pngfilename
      convert $pngfilename $tiffilename
      /bin/rm $pngfilename
   fi
done
