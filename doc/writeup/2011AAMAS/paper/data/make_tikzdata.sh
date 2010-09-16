#!/bin/bash
# Script to generate TikZ data files for the results specified
# Author: Dhirendra Singh

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../../src/distfiles/scripts/plot.sh


##### Results 468 #####
RESULTS=${SCRIPTDIR}/../../../../results/blak468
${PLOTSH} -d ${RESULTS} -t storage1 -o ./storage1b -w 500 -z 200
${PLOTSH} -d ${RESULTS} -t storage1m -o ./storage1mb -w 500 -z 200
${PLOTSH} -d ${RESULTS} -t storage2b -o ./storage2b -w 500 -z 200
${PLOTSH} -d ${RESULTS} -t storage2mb -o ./storage2mb -w 500 -z 200
