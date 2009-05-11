#!/bin/bash

#---------------------------
#---- Helper Functions -----
#---------------------------

function avg() {
touch $tmpdir/.paste
for file in $@
do
/usr/bin/cut -f2 -d' ' $file > $tmpdir/.cut
/usr/bin/paste $tmpdir/.cut $tmpdir/.paste > $tmpdir/.temp
/bin/mv $tmpdir/.temp $tmpdir/.paste
done
/usr/bin/awk '{avg=0; for(i=1; i<=NF; i++){avg+=$i}; avg/=NF; print avg}' $tmpdir/.paste > $tmpdir/.avg
/usr/bin/cut -f1 -d' ' $1 > $tmpdir/.cut
/usr/bin/paste $tmpdir/.cut $tmpdir/.avg
}

function makePlotCommands() {
# $1: outputfile,  $2: Stable data,  $3: Concurrent data,  $4: plotrange,  $5: label
cat << EOF
#set terminal epslatex color rounded size 4,2.5
set terminal postscript landscape color rounded 
#set output "$1.tex"
set output "$1"
set title "$5"
set xlabel "Iteration"
#set xtics 0, 250
set ylabel "Success"
set yrange [0:1]
set ytic 0, 0.2
set mytics 2
set grid noxtics ytics mytics
show grid
set key center right
set key spacing 1.3
plot $4 "$2" title "Stable" lt 2 lw 2 with lines, "$3" title "Concurrent" lt 1 lw 2 with lines
reset
EOF
}

parseargs()
{
HELP='
Usage: '`basename $0`' -d srcdir -t testname -o outfile -g gnuplot [-r plotrange]
       -d srcdir     Top-level directory containing test result files
       -t testname   Test name (must match srcdir/**/testname*stable*.csv and srcdir/**/testname*concurrent*.csv)
       -o outfile    File to store the plot results to (PDF format)
       -g gnuplot    Full path to gnuplot binary
       -l label      String to use as plot title
       -r plotrange  X-data range in gnuplot format to plot (optional - default is "[]")
'
range="[]"
label=""

args=`getopt t:d:o:r:g:l: $*`
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
if [ "$testname" == "" ] || [ "$srcdir" == "" ] || [ "$outfile" == "" ] || [ "$gnuplot" = "" ]
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
#echo gnuplot=$gnuplot
#echo label=$label
}


#---------------------------
#---- Main Script ----------
#---------------------------

#--- Parse the arguments
parseargs $@

#--- Temp directory
tmpdir=/tmp/`date "+%Y%m%d%H%M%S"`
/bin/mkdir $tmpdir

#--- Generate the averaged data to plot
set1=`find $srcdir -name "$testname*stable*.csv" -print`
set2=`find $srcdir -name "$testname*concurrent*.csv" -print`
avg $set1 > $tmpdir/.stable.data
avg $set2 > $tmpdir/.concurrent.data

#--- Generate the gnupot commands script
makePlotCommands $tmpdir/.gnuplot.eps $tmpdir/.stable.data $tmpdir/.concurrent.data $range > $tmpdir/.gnuplot.commands $label

#--- Plot and PDF
$gnuplot $tmpdir/.gnuplot.commands
ps2pdf -sPAPERSIZE=a4 -dEmbedAllFonts=true $tmpdir/.gnuplot.eps $outfile

#--- Cleanup
rm -rf $tmpdir
