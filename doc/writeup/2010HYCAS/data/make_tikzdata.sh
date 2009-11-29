#!/bin/bash

#####
# Script to generate TikZ data files for the results specified
# Author: dsingh
#####

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../src/distfiles/scripts/plot.sh


##### Results 291 #####
RESULTS=${SCRIPTDIR}/../../../results/blak291
${PLOTSH} -d ${RESULTS} -t hanoid5s1r8 -o ./hanoid5s1r8 -w 100 -z 450
${PLOTSH} -d ${RESULTS} -t hanoid5s3r8 -o ./hanoid5s3r8 -w 100 -z 450
${PLOTSH} -d ${RESULTS} -t hanoid5s5r8 -o ./hanoid5s5r8 -w 100 -z 450
