#!/usr/bin/env perl
use strict;
use warnings;
# Globals
use vars qw/ %opt /;

# Command line options processing
sub init()
{
    use Getopt::Std;
    my $opt_string = 'hi:o:w:';
    getopts( "$opt_string", \%opt ) or usage();
    usage() if not defined ($opt{i} and $opt{o} and $opt{w});
    usage() if $opt{h};
}

# Message about this program and how to use it
sub usage()
{
    print STDERR << "EOF";
usage: $0 [-h] [-i file] [-o file]
    
    -h        : this (help) message
    -i file   : file containing input values
    -o file   : output file
    -w window : moving average window size  
    
example: $0 -i data.txt -o data.avg -w 10
    
EOF
    exit;
}

# Start here
init();

open (IN, "<", "$opt{i}") or die "Couldn't open input file: $opt{i}";
open (OUT, ">", "$opt{o}") or die "Couldn't open output file: $opt{o}";

my $i=0;
my $l=0;
my @success;
my $successAvg=0;
my $window=$opt{w};
my @fields;

while (<IN>) {
    chomp;
    @fields = split(/ /, $_);    
    $success[$i-int($i/$window)*$window]=$fields[1];
    if($i>=$window){
        for($l=0;$l<$window;$l++){
            $successAvg+=$success[$l]/$window;
        }
        print OUT "$fields[0] $successAvg\n";
        $successAvg=0;
    } else {
        #print OUT "$fields[0] $fields[1] \n";
    }
    $i++;    
}
close (IN);
close (OUT);

