#! /usr/bin/env perl

$filenameIn = $ARGV[0];
$filenameOut = $ARGV[1];
$sequence = $ARGV[2];
$runtimes = $ARGV[3];
($trainSteps,$learnSteps) = split(/:/,$runtimes,2);
print $content."\n";
open(FH,$filenameIn) || die "Can not open file:$filename";
%result = 0;
while (<FH>)
{
	($time, $step)= /(\d*)\s*(\d*)/;
	$result{$time} = $step;
	
}
close(FH);
$trainSum = 0;
$learnSum = 0;
foreach (keys %result)
{
	if ( ($_ > 0) && ($_ <= $trainSteps) )	
	{
		$trainSum += $result{$_};
	}
	else	
	{
		$learnSum += $result{$_};
	}	
}
$train = $trainSum/$trainSteps;
$learn = $learnSum/$learnSteps;
$content = "$sequence\t$train\t$learn\n";
print $content."\n";
open(FH,">>",$filenameOut);
print FH $content;
close(FH);
