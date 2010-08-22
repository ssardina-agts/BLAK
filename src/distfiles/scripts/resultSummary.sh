#!/bin/bash

#--- Parse arguments
if [ "$2" == "" ]; then 
echo "usage: $0 <path-to-results-root-dir> <output-dir>"
exit
fi
path=$1
outpath=$2

#--- Helper function
getTreeInfo()
{
infile=$1
tail -10000 $infile | egrep "number of instances|Number of Leaves|Size of the tree|Outcomes for"
}

getTries()
{
infile=$1
tmpfile=/tmp/tmpfile$RANDOM.txt
grep "Recorded success for goal" $1 | cut -f1 -d'.' | sort | uniq  > $tmpfile
cat $tmpfile | while read line; do 
line2=`echo $line | sed -e 's/\[/\\\[/g' | sed -e 's/\]/\\\]/g'`
sed -n "/$line2/{p;q;}" $infile
done
rm $tmpfile
}

#--- Main 
for file in `find $1 -name "player.out" -print | grep concurrent`
do 
file2=$outpath/`echo $file | sed -e 's/.*results\///' | sed -e 's/\//-/g'`.txt
echo $file
getTreeInfo $file > $file2
getTries $file >> $file2
done
exit


