#! /usr/bin/env perl

$mode = $ARGV[0];
$filename = "./src/DeconstructivePlan.plan";
open (RFH,$filename) || die "Can not open that file" ;
@lines=<RFH>;
close (RFH);
if ($mode == 1)
{
	open (WFH,">",$filename) || die "Can not open that file" ;
	print WFH "";
	close (WFH);
	open (WFH,">>",$filename) || die "Can not open that file" ;
	foreach(@lines)
	{
		$_ = "//REMOVABLE\n" if /REMOVABLE/;
		print WFH $_;
	}	
	close (WFH);
}
if ($mode == 2)
{
	foreach(@lines)
	{
		$_ = "\t\t !ag.isGoalTower(first.getValue())&& //REMOVABLE \n" if /REMOVABLE/;
	}	
	open (WFH,">",$filename) || die "Can not open that file" ;
	print WFH @lines;
	close (WFH);
}


