#!/bin/bash

#####
# Script to generate TikZ data files for the results specified
# Author: dsingh
#####

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../../src/distfiles/scripts/plot.sh

##### Results 128 #####
RESULTS110=${SCRIPTDIR}/../../../../discussion/dp1-new-worlds/results/blak128
${PLOTSH} -d ${RESULTS110} -t test01v3gm -o ./test01v3gm -w 100 -z 250
${PLOTSH} -d ${RESULTS110} -t test05v3gm -o ./test05v3gm -w 100 -z 250

##### Results 110 #####
RESULTS110=${SCRIPTDIR}/../../../../discussion/dp1-new-worlds/results/blak110
#${PLOTSH} -d ${RESULTS110} -t test01v10gm -o ./test01v10gm -w 100 -z 600
#${PLOTSH} -d ${RESULTS110} -t test05v5gm -o ./test05v5gm -w 100 -z 400
${PLOTSH} -d ${RESULTS110} -t testImpactvars -o ./testImpactvars -w 100 -z 300
${PLOTSH} -d ${RESULTS110} -t testMultiSolutionsR -o ./testMultiSolutionsR -w 100 -z 1000
#######################
