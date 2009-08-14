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
# $1: outputfile,  $2: Stable data,  $3: Concurrent data,  $4: Coverage data,  $5: plotrange,  $6: label,  $7: every
if ( [ "$2" == "nil" ] && [ "$3" == "nil" ] && [ "$4" == "nil" ] ); then
    plotcmd=""
else 
    plotcmd="plot $5 "
fi
if [ "$2" != "nil" ]; then
    plotcmd+="\"$2\" every $7 title \"Stable\" lt 1 lw 2 with lines"
fi
if [ "$3" != "nil" ]; then
    if [ "$2" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$3\" every $7 title \"Concurrent\" lt 2 lw 2 with lines"
fi
if [ "$4" != "nil" ]; then
    if [ "$2" != "nil" ] || [ "$3" != "nil" ]; then
        plotcmd+=", "
    fi
    plotcmd+="\"$4\" every $7 title \"Coverage\" lt 3 lw 2 with lines"
fi

cat << EOF
#set terminal epslatex color rounded size 4,2.5
set terminal postscript landscape color rounded 
#set output "$1.tex"
set output "$1"
set title "$6"
set xlabel "Iteration"
#set xtics 0, 250
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
       -S            Look for Stable results (srcdir/**/testname*stable*.csv)
       -C            Look for Concurrent results (srcdir/**/testname*concurrent*.csv)
       -U            Look for Coverage results (srcdir/**/testname*coverage*.csv)
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
		-C)
			cmpC="concurrent";
			shift;;
		-S)
			cmpS="stable";
			shift;;
		-U)
			cmpU="coverage";
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
if [ "$testname" == "" ] || [ "$srcdir" == "" ] || [ "$outfile" == "" ] || [ "$gnuplot" == "" ] || (  [ "$cmpC" == "" ] && [ "$cmpS" == "" ] && [ "$cmpU" == "" ] )
then
	echo "$HELP"
	exit 65
fi
if [ ! -e $srcdir ]
then
	echo "Directory [$srcdir] does not exist. Exiting."
	exit -1
fi
#echo cmpC=$cmpC
#echo cmpS=$cmpS
#echo cmpU=$cmpU
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
if [ "$cmpS" != "" ]; then
set1=`find $srcdir -name "$testname-stable*.csv" -print`
fi
if [ "$cmpC" != "" ]; then
set2=`find $srcdir -name "$testname-concurrent*.csv" -print`
fi
if [ "$cmpU" != "" ]; then
set3=`find $srcdir -name "$testname-coverage*.csv" -print`
fi
if [ "$set1" == "" ] && [ "$set2" == "nil" ] && [ "$set3" == "nil" ] ; then
 echo "No data found for test $testname"
 exit 0
fi

#--- Generate the averaged results
if [ "$set1" == "" ]; then 
stableDataFile=nil 
else 
stableDataFile=$tmpdir/.stable.data
avg $set1 > $stableDataFile
`$mov_avg -i $stableDataFile -o $stableDataFile.mavg -w $window`
/bin/mv $stableDataFile.mavg $stableDataFile
fi
if [ "$set2" == "" ]; then 
concurrentDataFile=nil
else 
concurrentDataFile=$tmpdir/.concurrent.data
avg $set2 > $concurrentDataFile
`$mov_avg -i $concurrentDataFile -o $concurrentDataFile.mavg -w $window`
/bin/mv $concurrentDataFile.mavg $concurrentDataFile
fi
if [ "$set3" == "" ]; then 
coverageDataFile=nil
else 
coverageDataFile=$tmpdir/.coverage.data
avg $set3 > $coverageDataFile
`$mov_avg -i $coverageDataFile -o $coverageDataFile.mavg -w $window`
/bin/mv $coverageDataFile.mavg $coverageDataFile
fi

#--- Generate the gnuplot commands script
makePlotCommands $tmpdir/.gnuplot.eps $stableDataFile $concurrentDataFile $coverageDataFile $range > $tmpdir/.gnuplot.commands $label $every

#--- Plot and PDF
$gnuplot $tmpdir/.gnuplot.commands
ps2pdf -sPAPERSIZE=a4 -dEmbedAllFonts=true $tmpdir/.gnuplot.eps $outfile

#--- Cleanup
rm -rf $tmpdir
