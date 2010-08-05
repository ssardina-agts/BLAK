#!/bin/bash

mov_avg="$(cd "${0%/*}" 2>/dev/null; echo "$PWD")"/movingAvg.pl

#---------------------------
#---- Helper Functions -----
#---------------------------

parseargs()
{
filename=""
value="never"

HELP='
Usage: '`basename $0`' -f file -o value
       -f file    JACK source file containing the failure recovery setting
                  (change will be made in-file)
       -o value   'never' or 'repost'
'

args=`getopt f:o: $*`
set -- $args
for i
do
	case "$i" in
		-f)
			filename="$2"; shift;
			shift;;
		-o)
			value="$2"; shift;
			shift;;
		--)
			shift; break;;
	esac
done
if [ "$filename" == "" ]
then
	echo "$HELP"
	exit 65
fi
}


#---------------------------
#---- Main Script ----------
#---------------------------

#--- Parse the arguments
parseargs $@

if [ "$value" == "repost" ]
then
fromstr="never";
else 
fromstr="repost";
fi

sed -e "s/#set behavior Recover $fromstr;/#set behavior Recover $value;/" $filename > $filename.tmp
mv $filename.tmp $filename

