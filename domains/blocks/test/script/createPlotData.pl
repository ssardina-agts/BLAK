#! /usr/bin/env perl
$filename = $ARGV[0];
$experimentTimes = $ARGV[1];

$content ="";
$content .= "set terminal postscript landscape color rounded\n";
$content .= "set grid\n";
#$content .= "set xrange [0:3]\n";
$content .= 'set xtics ("Training (Random Selection)" 1, "Testing (Learned Selection)" 2)'."\n";
#$content .= 'set xlabel "Run times"'."\n";
$content .= 'set ylabel "Moves (Average)"'."\n";
$content .= 'set output "'.$filename.'.eps"'."\n" ;	

$trainSum = 0;
$learnSum = 0;

open(FH,$filename)|| (die "Can not that Original file:$filename\n");
while ($line=<FH>)
{
	($times,$trainSteps,$learnSteps) = split(/\t/,$line,3);
	$trainSum += $trainSteps;
	$learnSum += $learnSteps;
}
close FH;

$trainAverage = $trainSum/$experimentTimes; # change
$learnAverage = $learnSum/$experimentTimes; # change
$min = int($learnAverage) - 1;
$max = int($trainAverage) + 2;

$content .="set yrange [0:$max]\n";
$content .= 'plot [0:3] "'.$filename.'" with boxes fs solid lc rgb "blue"'."\n";
$content .= "\n";

$width = 0.4;

open(FH,">",$filename)|| die "Can not that file";
#print FH "0\t$min\t$width\n1\t$trainAverage\t$width\n2\t$learnAverage\t$width\n3\t$min\t$width\n";
print FH "1\t$trainAverage\t$width\n2\t$learnAverage\t$width\n";
close FH;

#create plot script
$plotfile=$filename.".plot";
open(FH,">",$plotfile);
print FH $content;
close(FH);
