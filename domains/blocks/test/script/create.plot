set terminal jpeg
set grid
set xrange [0:3]
set xtics ("TrainStepsAverage" 1, "LearnStepsAverage" 2)
set xlabel "Run times"
set ylabel "Steps used"
set output "../Experiment/Experiment4/Experiment4_Result.jpeg"
set yrange [65:70]
plot [0:3] "../Experiment/Experiment4/Experiment4_Result.bak" with boxes

set output "../Experiment/Experiment3/Experiment3_Result.jpeg"
set yrange [12:15]
plot [0:3] "../Experiment/Experiment3/Experiment3_Result.bak" with boxes

set output "../Experiment/Experiment1/Experiment1_Result.jpeg"
set yrange [1:4]
plot [0:3] "../Experiment/Experiment1/Experiment1_Result.bak" with boxes

set output "../Experiment/Experiment5/Experiment5_Result.jpeg"
set yrange [9:12]
plot [0:3] "../Experiment/Experiment5/Experiment5_Result.bak" with boxes

set output "../Experiment/Experiment2/Experiment2_Result.jpeg"
set yrange [12:15]
plot [0:3] "../Experiment/Experiment2/Experiment2_Result.bak" with boxes

