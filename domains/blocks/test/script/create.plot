set terminal jpeg
set grid
set xrange [0:3]
set xtics ("TrainStepsAverage" 1, "LearnStepsAverage" 2)
set xlabel "Run times"
set ylabel "Steps used"
set output "../../result/Experiment4/Experiment4_Result.jpeg"
set yrange [63:72]
plot [0:3] "../../result/Experiment4/Experiment4_Result.bak" with boxes

set output "../../result/Experiment3/Experiment3_Result.jpeg"
set yrange [12:14]
plot [0:3] "../../result/Experiment3/Experiment3_Result.bak" with boxes

set output "../../result/Experiment1/Experiment1_Result.jpeg"
set yrange [1:4]
plot [0:3] "../../result/Experiment1/Experiment1_Result.bak" with boxes

set output "../../result/Experiment5/Experiment5_Result.jpeg"
set yrange [9:12]
plot [0:3] "../../result/Experiment5/Experiment5_Result.bak" with boxes

set output "../../result/Experiment2/Experiment2_Result.jpeg"
set yrange [12:15]
plot [0:3] "../../result/Experiment2/Experiment2_Result.bak" with boxes

