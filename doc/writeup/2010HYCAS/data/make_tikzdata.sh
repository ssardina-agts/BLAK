#!/bin/bash

#####
# Script to generate TikZ data files for the results specified
# Author: dsingh
#####

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
#PLOTSH=${SCRIPTDIR}/../../../../src/distfiles/scripts/plot.sh
PLOTSH=./plot.sh

function scaleXResults () { mv $1 .tmpfile && awk '{ print $1/2 " " $2}'  .tmpfile > $1 && rm .tmpfile ; }
function scaleYResults () { mv $1 .tmpfile && awk '{ print $1 " " $2/2}'  .tmpfile > $1 && rm .tmpfile ; }


##### Results 295 #####
#RESULTS=${SCRIPTDIR}/../../../results/blak295
RESULTS=${SCRIPTDIR}/../../../results/blak295+

${PLOTSH} -d ${RESULTS} -t hanoid5s1r8 -o ./hanoid5s1r8 -w 100 -z 485
${PLOTSH} -d ${RESULTS} -t hanoid5s3r8 -o ./hanoid5s3r8 -w 100 -z 485
${PLOTSH} -d ${RESULTS} -t hanoid5s5r8 -o ./hanoid5s5r8 -w 100 -z 485
${PLOTSH} -d ${RESULTS} -t hanoi5 -o ./hanoi5 -w 200 -z 1975
${PLOTSH} -d ${RESULTS} -t hanoi5t -o ./hanoi5t -w 200 -z 1975

${PLOTSH} -d ${RESULTS} -t hanoi5s -o ./hanoi5s -w 1 -z 5

# Tex cannot handle >16383 numbers so scale results here
scaleXResults hanoi5.CF.tikzdata
scaleXResults hanoi5.CP.tikzdata
scaleXResults hanoi5t.CF.tikzdata
scaleXResults hanoi5t.CP.tikzdata
scaleYResults hanoi5s.CF.tikzdata
scaleYResults hanoi5s.CP.tikzdata
