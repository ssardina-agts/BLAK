#!/bin/bash

#####
# Script to generate TikZ data files for the results specified
# Author: dsingh
#####

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../../src/distfiles/scripts/plot.sh

##### Results 146 #####
RESULTS=${SCRIPTDIR}/../../../../results/blak146
${PLOTSH} -d ${RESULTS} -t test01v3gm -o ./test01v3gm -w 100 -z 250

##### Results 137 #####
RESULTS=${SCRIPTDIR}/../../../../results/blak137
${PLOTSH} -d ${RESULTS} -t testImpactvars2 -o ./testImpactvars2 -w 100 -z 450
#${PLOTSH} -d ${RESULTS} -t testImpactvars3 -o ./testImpactvars3 -w 100 -z 250

##### Results 130 #####
RESULTS=${SCRIPTDIR}/../../../../results/blak130
#${PLOTSH} -d ${RESULTS} -t test01v3gm -o ./test01v3gm -w 100 -z 250
${PLOTSH} -d ${RESULTS} -t test05v3gm -o ./test05v3gm -w 100 -z 250

##### Results 110 #####
RESULTS=${SCRIPTDIR}/../../../../results/blak110
#${PLOTSH} -d ${RESULTS} -t test01v10gm -o ./test01v10gm -w 100 -z 600
#${PLOTSH} -d ${RESULTS} -t test05v5gm -o ./test05v5gm -w 100 -z 400
#${PLOTSH} -d ${RESULTS} -t testImpactvars -o ./testImpactvars -w 100 -z 300
${PLOTSH} -d ${RESULTS} -t testMultiSolutionsR -o ./testMultiSolutionsR -w 100 -z 1000
#######################
