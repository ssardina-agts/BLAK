#!/bin/bash

#####
# Script to generate TikZ data files for the results specified
# Author: dsingh
#####

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../src/distfiles/scripts/plot.sh


##### Add line numbers (tcsh format) 
#foreach file ( `find . -name "*csv" -print` )
#awk '{print FNR, $0}' $file > $file.tmp
#mv $file.tmp $file
#end


##### Make results #####
RESULTS=${SCRIPTDIR}/.
${PLOTSH} -d ${RESULTS} -t testfr01a -o ./testfr01a -w 5 -z 100
${PLOTSH} -d ${RESULTS} -t testfr01b -o ./testfr01b -w 5 -z 100
${PLOTSH} -d ${RESULTS} -t testfr01na -o ./testfr01na -w 5 -z 100
${PLOTSH} -d ${RESULTS} -t testfr01nb -o ./testfr01nb -w 5 -z 100


### Take the first N samples ####
HEAD=10
for file in `find . -name "*tikzdata" -print`
do
head -${HEAD} $file > $file.tmp
mv $file.tmp $file
done
