all: lib/jack.jar lib/weka.jar lib/blak.jar clean lib/batterymanager.jar

lib/jack.jar:
	@echo "JACK bundle not found in $@. Please copy it here before continuing."
	@exit 1

lib/weka.jar:
	@echo "WEKA bundle not found in $@. Please copy it here before continuing."
	@exit 1

lib/blak.jar: ../../build/dist/lib/blak.jar
	cp $? $@

lib/batterymanager.jar:
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar aos.main.JackBuild -wd src -r -d ../build -i -DJAVACARGS=-g
	cd build && jar -cvf ../lib/batterymanager.jar *

clean:
	/bin/rm -rf build lib/batterymanager.jar

help: lib/batterymanager.jar
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar:lib/batterymanager.jar BatteryManager -h

run: lib/batterymanager.jar
	java -cp lib/jack.jar:lib/weka.jar:lib/blak.jar:lib/batterymanager.jar BatteryManager -u concurrent -s confidence -o bm -n 5 -k 1 -e 0.5 -a 0.5 -v 5 -w 5 -l 20 -m 0 -c 0 -f 0 -b 0.0 -i 1000

