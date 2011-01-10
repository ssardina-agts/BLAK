#!/bin/bash
# Script to generate TikZ data files for the results specified
# Author: Dhirendra Singh

SCRIPTDIR="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"
PLOTSH=${SCRIPTDIR}/../../../../../src/distfiles/scripts/plot.sh


RUN=blak475
RESULTS=${SCRIPTDIR}/../../../../results/${RUN}
${PLOTSH} -d ${RESULTS} -t storage1 -o ./storage1b -w 500 -z 200
${PLOTSH} -d ${RESULTS} -t storage1m -o ./storage1mb -w 500 -z 200
${PLOTSH} -d ${RESULTS} -t storage2b -o ./storage2b -w 500 -z 200
${PLOTSH} -d ${RESULTS} -t storage2mb -o ./storage2mb -w 500 -z 200
${PLOTSH} -d ${RESULTS} -t storage3mb -o ./storage3mb -w 500 -z 200
# Zooming on the first 100 points in storage3mb that are interesting.
head -100 storage3mb.CF.tikzdata > tmp
mv tmp storage3mb.CF.tikzdata
head -100 storage3mb.CP.tikzdata > tmp
mv tmp storage3mb.CP.tikzdata

#--- Average DT instances for all plans over five runs for test storage2 and storage 2m
# $ grep "instances :" ./results/storage2/concurrent-c*/sto*.out | awk '{ SUM += $5} END { print SUM/5 }'
# 56590.2
# $ grep "instances :" ./results/storage2m/concurrent-c*/sto*.out | awk '{ SUM += $5} END { print SUM/5 }'
# 14708.8

#--- Average number of calls to plan Execute over five runs for test storage2 and storage 2b
# $ grep "Plan Execute is operating" ./results/storage2/concurrent-c*/sto*.out | wc -l | awk '{ SUM += $1} END { print SUM/5 }'
# 40000
# $ grep "Plan Execute is operating" ./results/storage2b/concurrent-c*/sto*.out | wc -l | awk '{ SUM += $1} END { print SUM/5 }'
# 35203