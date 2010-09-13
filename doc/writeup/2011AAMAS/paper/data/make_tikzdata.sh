#!/bin/bash

#####
# Script to generate TikZ data files for the results specified
# Author: dsingh
#####

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../../src/distfiles/scripts/plot.sh


##### Results 181 #####
#RESULTS=${SCRIPTDIR}/../../../../results/blak181
#${PLOTSH} -d ${RESULTS} -t test01v3gm -o ./test01v3gm -w 100 -z 150
