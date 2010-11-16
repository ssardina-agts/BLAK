all: lib/jack.jar lib/weka.jar lib/blak.jar clean lib/bwagent.jar

lib/jack.jar:
	@echo "JACK bundle not found in $@. Please copy it here before continuing."
	@exit 1

lib/weka.jar:
	@echo "WEKA bundle not found in $@. Please copy it here before continuing."
	@exit 1

lib/blak.jar:
	@echo "BDI Learning bundle not found in $@. Please copy it here before continuing."
	@exit 1

lib/bwagent.jar:
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar aos.main.JackBuild -wd src -r -d ../build -i -DJAVACARGS=-g
	cd build && jar -cvf ../lib/bwagent.jar *

clean:
	/bin/rm -rf build/* lib/bwagent.jar

help: lib/bwagent.jar
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar:lib/bwagent.jar BWAgent -h

run: lib/bwagent.jar
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar:lib/bwagent.jar BWAgent -u concurrent -s confidence -o bw -k 1 -e 0.5 -a 0.5 -v 5 -w 5 -l 20 -m 0 -b 0.0 -i 1000

