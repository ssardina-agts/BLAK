#!/bin/bash

#####
# Script to generate TikZ data files for the results specified
# Author: dsingh
#####

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../src/distfiles/scripts/plot.sh

function scaleResults () { mv $1 .tmpfile && awk '{ print $1/2 " " $2}'  .tmpfile > $1 && rm .tmpfile ; }


##### HYCAS Results (uncomment if required) #####
#ln -s ../../../writeup/2010HYCAS/data/hanoi5.CF.tikzdata hanoi5.CF.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoi5.CP.tikzdata hanoi5.CP.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoi5t.CF.tikzdata hanoi5t.CF.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoi5t.CP.tikzdata hanoi5t.CP.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoid5s1r8.CF.tikzdata hanoid5s1r8.CF.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoid5s1r8.CP.tikzdata hanoid5s1r8.CP.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoid5s3r8.CF.tikzdata hanoid5s3r8.CF.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoid5s3r8.CP.tikzdata hanoid5s3r8.CP.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoid5s5r8.CF.tikzdata hanoid5s5r8.CF.tikzdata
#ln -s ../../../writeup/2010HYCAS/data/hanoid5s5r8.CP.tikzdata hanoid5s5r8.CP.tikzdata


##### Results 323 #####
RESULTS=${SCRIPTDIR}/../../../results/blak323
${PLOTSH} -d ${RESULTS} -t hanoid5s1r8 -o ./hanoid5s1r8new -w 100 -z 485
${PLOTSH} -d ${RESULTS} -t hanoid5s3r8 -o ./hanoid5s3r8new -w 100 -z 485
${PLOTSH} -d ${RESULTS} -t hanoid5s5r8 -o ./hanoid5s5r8new -w 100 -z 485
${PLOTSH} -d ${RESULTS} -t hanoi5 -o ./hanoi5new -w 200 -z 1975
${PLOTSH} -d ${RESULTS} -t hanoi5t -o ./hanoi5tnew -w 200 -z 1975

# Tex cannot handle >16383 numbers so scale results here
scaleResults hanoi5new.CF.tikzdata
scaleResults hanoi5new.CP.tikzdata
scaleResults hanoi5tnew.CF.tikzdata
scaleResults hanoi5tnew.CP.tikzdata
