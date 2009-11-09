all: lib/jack.jar lib/weka.jar lib/blak.jar clean lib/hanoi.jar

lib/jack.jar:
	@echo "JACK bundle not found in ./lib/jack.jar. Please copy it here before continuing."
	@exit 1

lib/weka.jar:
	@echo "WEKA bundle not found in ./lib/weka.jar. Please copy it here before continuing."
	@exit 1

lib/blak.jar:
	@echo "BDI-Learning bundle not found in ./lib/blak.jar. Please copy it here before continuing."
	@exit 1

lib/hanoi.jar:
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar aos.main.JackBuild -wd src -r -d ../build
	cd build && jar -cvf ../lib/hanoi.jar *

clean:
	/bin/rm -rf build lib/hanoi.jar

run: lib/hanoi.jar
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar:lib/hanoi.jar Player -g 123456 -r 15 -j 9 -i 10 -w

runl: lib/hanoi.jar
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar:lib/hanoi.jar Player -x -u concurrent -s coverage -d . -o player.csv -g 123456 -r 2 -j 4 -i 3000

