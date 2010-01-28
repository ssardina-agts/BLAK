#!/bin/bash

###
# Script to generate TikZ data files for the results specified
# Author: dsingh
#

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../src/distfiles/scripts/plot.sh


### Make results #####
RESULTS=${SCRIPTDIR}/../../../results/blak343/blak/results/all

### Add line numbers to the raw data files:
for file in `find ${RESULTS} -name "testfr*topgoal.csv" -print`
do
file1=$(basename $file)
awk '{print FNR, $0}' $file > ${SCRIPTDIR}/$file1
done

### Generate the data files for tikZ
${PLOTSH} -d ${SCRIPTDIR} -t testfr01a -o ./testfr01a -w 5 -z 50
${PLOTSH} -d ${SCRIPTDIR} -t testfr01b -o ./testfr01b -w 5 -z 50
${PLOTSH} -d ${SCRIPTDIR} -t testfr01na -o ./testfr01na -w 5 -z 50
${PLOTSH} -d ${SCRIPTDIR} -t testfr01nb -o ./testfr01nb -w 5 -z 50

### Take first N samples for plotting since size can vary
HEAD=10
for file in `find . -name "*tikzdata" -print`
do
head -${HEAD} $file > ${SCRIPTDIR}/$file.tmp
mv $file.tmp $file
done

### Remove the temp files
for file in `find ${RESULTS} -name "testfr*topgoal.csv" -print`
do
file1=$(basename $file)
/bin/rm ${SCRIPTDIR}/$file1
done

###