#!/bin/bash

mov_avg="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"/movingAvg.pl

#---------------------------
#---- Helper Functions -----
#---------------------------

function makeTikZ() {
awk -v c="$2" '{if (count++%c==0) print $0;}' $1
}

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
# $1: outputfile,  $2: plotrange,  $3: label,  $4: every,  $5:key location,  $6: StableP data,  $7: StableC data,  $8: ConcurrentP data,  $9: ConcurrentP data,  $10: confidence data
local o=$1
local r=$2
local l=$3
local e=$4
local k=$5
local sp=$6
local sc=$7
local cp=$8
local cc=$9
local bd=${10}
if ( [ "$bd" == "nil" ] && [ "$sp" == "nil" ] && [ "$sc" == "nil" ] && [ "$cp" == "nil" ] && [ "$cc" == "nil" ]); then
    plotcmd=""
else 
    plotcmd="plot $r "
fi
if [ "$cc" != "nil" ]; then
    plotcmd+="\"$cc\" every $e title \"ACL+Covr\" lt 1 pt 4 lw 2 lc rgb 'dark-magenta' with linespoints"
fi
if [ "$cp" != "nil" ]; then
    if [ "$cc" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$cp\" every $e title \"ACL+Prob\" lt 5 pt 8 lw 3 lc rgb 'red' with linespoints"
fi
if [ "$sc" != "nil" ]; then
    if [ "$cc" != "nil" ] || [ "$cp" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$sc\" every $e title \"BUL+Covr\" lt 8 pt 10 lw 3 lc rgb 'blue' with linespoints"
fi
if [ "$sp" != "nil" ]; then
    if [ "$cc" != "nil" ] || [ "$cp" != "nil" ] || [ "$sc" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$sp\" every $e title \"BUL+Prob\" lt 3 pt 6 lw 3 lc rgb 'dark-green' with linespoints"
fi
if [ "$bd" != "nil" ]; then
    if [ "$cc" != "nil" ] || [ "$cp" != "nil" ] || [ "$sc" != "nil" ] || [ "$sp" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$bd\" every $e title \"ACL+Conf\" lt 0 pt 3 lw 4 lc rgb 'black' with linespoints"
fi


cat << EOF
#set terminal epslatex color rounded size 4,2.5
set terminal postscript landscape color rounded 
set output "$o"
set title "$l"
set xlabel "Iteration"
set ylabel "Success"
set yrange [-0.1:1.1]
set ytic 0, 0.2
set mytics 2
set grid noxtics ytics mytics
show grid
set key $k
set key spacing 1.3
set pointsize 2
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
j=bottom
k=right
tikz=0

HELP='
Usage: '`basename $0`' -d srcdir -t testname -o outfile -g gnuplot [-r plotrange] [-e N]
       -d srcdir     Top-level directory containing test result files
       -t testname   Test name (must match srcdir/**/testname**-episodes.csv)
       -o outfile    File to store the plot results to (PDF format)
       -g gnuplot    Full path to gnuplot binary
       -l label      String to use as plot title
       -j loc1       Legend key location
       -k loc2       Legend key location
       -r plotrange  X-data range in gnuplot format to plot (optional - default is "'$range'")
       -e N          Plot every N point (optional - default is '$every')
       -w N          Use moving average window of size N (optional - default is '$window')
       -z N          Generate TikZ data file with every N point
'

args=`getopt t:d:o:r:e:w:g:l:j:k:z: $*`
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
		-j)
			j=$2; shift;
			shift;;
		-k)
			k=$2; shift;
			shift;;
		-z)
			tikz=$2; shift;
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
keyloc="$j $k"

#echo outfile=$outfile
#echo srcdir=$srcdir
#echo testname=$testname
#echo range=$range
#echo every=$every
#echo window=$window
#echo gnuplot=$gnuplot
#echo label=$label
#echo keyloc=$keyloc
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
setB=`find $srcdir -name "$testname-concurrent-confidence*-episodes.csv" -print`
set1p=`find $srcdir -name "$testname-stable-probabilistic*-episodes.csv" -print`
set1c=`find $srcdir -name "$testname-stable-coverage*-episodes.csv" -print`
set2p=`find $srcdir -name "$testname-concurrent-probabilistic*-episodes.csv" -print`
set2c=`find $srcdir -name "$testname-concurrent-coverage*-episodes.csv" -print`
if [ "$set1p" == "" ] && [ "$set1c" == "nil" ] && [ "$set2p" == "nil" ] && [ "$set2c" == "nil" ]; then
 echo "No data found for test $testname"
 exit 0
fi

#--- Generate the averaged results
if [ "$setB" == "" ]; then 
confidence=nil 
else 
confidence=$tmpdir/.confidencedata
avg $setB > $confidence
`$mov_avg -i $confidence -o $confidence.mavg -w $window`
/bin/mv $confidence.mavg $confidence
if [ "$tikz" -ne "0" ]; then
makeTikZ $confidence $tikz > $outfile.CF.tikzdata
fi
fi
if [ "$set1p" == "" ]; then 
stableP=nil 
else 
stableP=$tmpdir/.stable.pdata
avg $set1p > $stableP
`$mov_avg -i $stableP -o $stableP.mavg -w $window`
/bin/mv $stableP.mavg $stableP
if [ "$tikz" -ne "0" ]; then
makeTikZ $stableP $tikz > $outfile.SP.tikzdata
fi
fi
if [ "$set1c" == "" ]; then 
stableC=nil 
else 
stableC=$tmpdir/.stable.cdata
avg $set1c > $stableC
`$mov_avg -i $stableC -o $stableC.mavg -w $window`
/bin/mv $stableC.mavg $stableC
if [ "$tikz" -ne "0" ]; then
makeTikZ $stableC $tikz > $outfile.SC.tikzdata
fi
fi

if [ "$set2p" == "" ]; then 
concurrentP=nil
else 
concurrentP=$tmpdir/.concurrent.pdata
avg $set2p > $concurrentP
`$mov_avg -i $concurrentP -o $concurrentP.mavg -w $window`
/bin/mv $concurrentP.mavg $concurrentP
if [ "$tikz" -ne "0" ]; then
makeTikZ $concurrentP $tikz > $outfile.CP.tikzdata
fi
fi
if [ "$set2c" == "" ]; then 
concurrentC=nil
else 
concurrentC=$tmpdir/.concurrent.cdata
avg $set2c > $concurrentC
`$mov_avg -i $concurrentC -o $concurrentC.mavg -w $window`
/bin/mv $concurrentC.mavg $concurrentC
if [ "$tikz" -ne "0" ]; then
makeTikZ $concurrentC $tikz > $outfile.CC.tikzdata
fi
fi

#--- Plot and PDF
if [ "$tikz" == "0" ]; then
makePlotCommands $tmpdir/.gnuplot.eps $range $label $every "$keyloc" $stableP $stableC $concurrentP $concurrentC $confidence  > $tmpdir/.gnuplot.commands 
$gnuplot $tmpdir/.gnuplot.commands
ps2pdf -sPAPERSIZE=a4 -dEmbedAllFonts=true $tmpdir/.gnuplot.eps $outfile
fi

#--- Cleanup
rm -rf $tmpdir
