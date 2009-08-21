#!/bin/bash

mov_avg="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"/movingAvg.pl

#---------------------------
#---- Helper Functions -----
#---------------------------


function avg() {
rm -f $tmpdir/.paste
touch $tmpdir/.paste
for file in $@
do
/usr/bin/cut -f2 -d' ' $file > $tmpdir/.cut
/usr/bin/paste $tmpdir/.cut $tmpdir/.paste > $tmpdir/.temp
/bin/mv $tmpdir/.temp $tmpdir/.paste
done
/usr/bin/awk '{avg=0; for(i=1; i<=NF; i++){avg+=$i}; avg/=NF; print avg}' $tmpdir/.paste > $tmpdir/.avg
/usr/bin/cut -f1 -d' ' $1 > $tmpdir/.cut
/usr/bin/paste -d' ' $tmpdir/.cut $tmpdir/.avg
}

function makePlotCommands() {
# $1: outputfile,  $2: plotrange,  $3: label,  $4: every,  $5: StableP data,  $6: StableC data,  $7: ConcurrentP data,  $8: ConcurrentP data
local o=$1
local r=$2
local l=$3
local e=$4
local sp=$5
local sc=$6
local cp=$7
local cc=$8
if ( [ "$sp" == "nil" ] && [ "$sc" == "nil" ] && [ "$cp" == "nil" ] && [ "$cc" == "nil" ]); then
    plotcmd=""
else 
    plotcmd="plot $r "
fi
if [ "$cp" != "nil" ]; then
    plotcmd+="\"$cp\" every $e title \"Concurrent+P\" lt 1 lw 2 with lines"
fi
if [ "$sp" != "nil" ]; then
    if [ "$cp" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$sp\" every $e title \"Stable+P\" lt 2 lw 2 with lines"
fi
if [ "$cc" != "nil" ]; then
    if [ "$cp" != "nil" ] || [ "$sp" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$cc\" every $e title \"Concurrent+C\" lt 3 lw 2 with lines"
fi
if [ "$sc" != "nil" ]; then
    if [ "$cp" != "nil" ] || [ "$sp" != "nil" ] || [ "$cc" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$sc\" every $e title \"Stable+C\" lt 4 lw 2 with lines"
fi

cat << EOF
#set terminal epslatex color rounded size 4,2.5
set terminal postscript landscape color rounded 
set output "$o"
set title "$l"
set xlabel "Iteration"
set ylabel "Success"
set yrange [0:1.1]
set ytic 0, 0.2
set mytics 2
set grid noxtics ytics mytics
show grid
set key center right
set key spacing 1.3
$plotcmd
reset
EOF
}

parseargs()
{

range="[]"
label=""
every=1
window=1
gnuplot=gnuplot

HELP='
Usage: '`basename $0`' -d srcdir -t testname -o outfile -g gnuplot [-r plotrange] [-e N]
       -d srcdir     Top-level directory containing test result files
       -t testname   Test name (must match srcdir/**/testname**.csv)
       -o outfile    File to store the plot results to (PDF format)
       -g gnuplot    Full path to gnuplot binary
       -l label      String to use as plot title
       -r plotrange  X-data range in gnuplot format to plot (optional - default is "'$range'")
       -e N          Plot every N point (optional - default is '$every')
       -w N          Use moving average window of size N (optional - default is '$window')
'

args=`getopt t:d:o:r:e:w:g:l:SCU $*`
set -- $args
for i
do
	case "$i" in
		-t)
			testname="$2"; shift;
			shift;;
		-d)
			srcdir="$2"; shift;
			shift;;
		-o)
			outfile="$2"; shift;
			shift;;
		-r)
			range="$2"; shift;
			shift;;
		-e)
			every=$2; shift;
			shift;;
		-w)
			window=$2; shift;
			shift;;
		-g)
			gnuplot="$2"; shift;
			shift;;
		-l)
			label="$2"; shift;
			shift;;
		--)
			shift; break;;
	esac
done
if [ "$testname" == "" ] || [ "$srcdir" == "" ] || [ "$outfile" == "" ] || [ "$gnuplot" == "" ]
then
	echo "$HELP"
	exit 65
fi
if [ ! -e $srcdir ]
then
	echo "Directory [$srcdir] does not exist. Exiting."
	exit -1
fi
#echo outfile=$outfile
#echo srcdir=$srcdir
#echo testname=$testname
#echo range=$range
#echo every=$every
#echo window=$window
#echo gnuplot=$gnuplot
#echo label=$label
}


#---------------------------
#---- Main Script ----------
#---------------------------

#--- Parse the arguments
parseargs $@

#--- Create temp directory
tmpdir=/tmp/`date "+%Y%m%d%H%M%S"`
/bin/mkdir $tmpdir

#--- Collect the result files
set1p=`find $srcdir -name "$testname-stable-probabilistic*.csv" -print`
set1c=`find $srcdir -name "$testname-stable-coverage*.csv" -print`
set2p=`find $srcdir -name "$testname-concurrent-probabilistic*.csv" -print`
set2c=`find $srcdir -name "$testname-concurrent-coverage*.csv" -print`
if [ "$set1p" == "" ] && [ "$set1c" == "nil" ] && [ "$set2p" == "nil" ] && [ "$set2c" == "nil" ]; then
 echo "No data found for test $testname"
 exit 0
fi

#--- Generate the averaged results
if [ "$set1p" == "" ]; then 
stableP=nil 
else 
stableP=$tmpdir/.stable.pdata
avg $set1p > $stableP
`$mov_avg -i $stableP -o $stableP.mavg -w $window`
/bin/mv $stableP.mavg $stableP
fi
if [ "$set1c" == "" ]; then 
stableC=nil 
else 
stableC=$tmpdir/.stable.cdata
avg $set1c > $stableC
`$mov_avg -i $stableC -o $stableC.mavg -w $window`
/bin/mv $stableC.mavg $stableC
fi

if [ "$set2p" == "" ]; then 
concurrentP=nil
else 
concurrentP=$tmpdir/.concurrent.pdata
avg $set2p > $concurrentP
`$mov_avg -i $concurrentP -o $concurrentP.mavg -w $window`
/bin/mv $concurrentP.mavg $concurrentP
fi
if [ "$set2c" == "" ]; then 
concurrentP=nil
else 
concurrentC=$tmpdir/.concurrent.cdata
avg $set2c > $concurrentC
`$mov_avg -i $concurrentC -o $concurrentC.mavg -w $window`
/bin/mv $concurrentC.mavg $concurrentC
fi

#--- Generate the gnuplot commands script
makePlotCommands $tmpdir/.gnuplot.eps $range $label $every $stableP $stableC $concurrentP $concurrentC  > $tmpdir/.gnuplot.commands 

#--- Plot and PDF
$gnuplot $tmpdir/.gnuplot.commands
ps2pdf -sPAPERSIZE=a4 -dEmbedAllFonts=true $tmpdir/.gnuplot.eps $outfile

#--- Cleanup
rm -rf $tmpdir
