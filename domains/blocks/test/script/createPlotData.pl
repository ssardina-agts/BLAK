#! /usr/bin/env perl

opendir(DH,"../../result") || die "Can not open directory";
@files = readdir DH;
closedir DH;
$content ="";
$content .= "set terminal jpeg\n";
$content .= "set grid\n";
$content .= "set xrange [0:3]\n";
$content .= 'set xtics ("TrainStepsAverage" 1, "LearnStepsAverage" 2)'."\n";
$content .= 'set xlabel "Run times"'."\n";
$content .= 'set ylabel "Steps used"'."\n";
foreach(@files)
{
	next if /\./;
	$filename = "../../result/$_/".$_."_Result";
	$content .= 'set output "'.$filename.'.jpeg"'."\n" ;
	open(FH,$filename)|| die "Can not that file";	
	$trainSum = 0;
	$learnSum = 0;
	while ($line=<FH>)
	{
		($times,$trainSteps,$learnSteps) = split(/\t/,$line,3);
		$trainSum += $trainSteps;
		$learnSum += $learnSteps;
	}
	close FH;
	$trainAverage = $trainSum/5;
	$learnAverage = $learnSum/5;
	$min = int($learnAverage) - 1;
	$max = int($trainAverage) + 2;
	$content .="set yrange [$min:$max]\n";
	$content .= 'plot [0:3] "'.$filename.".bak".'" with boxes'."\n";
	$content .= "\n";
	$filename .= ".bak";
	$width = 0.4;
	open(FH,">",$filename)|| die "Can not that file";
	print FH "0\t$min\t$width\n1\t$trainAverage\t$width\n2\t$learnAverage\t$width\n3\t$min\t$width\n";
	close FH;
}
#create plot script

open(FH,">","create.plot");
print FH $content;
close(FH);
