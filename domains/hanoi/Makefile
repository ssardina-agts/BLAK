all: lib/jack.jar clean lib/hanoi.jar

lib/jack.jar:
	@echo "JACK bundle not found in ./lib/jack.jar. Please copy it here before continuing."
	@exit 1

lib/hanoi.jar:
	java -cp lib/jack.jar aos.main.JackBuild -wd src -r -d ../build
	cd build && jar -cvf ../lib/hanoi.jar *

clean:
	/bin/rm -rf build lib/hanoi.jar

run: lib/hanoi.jar
	java -cp lib/jack.jar:lib/hanoi.jar Player

run-w: lib/hanoi.jar
	java -cp lib/jack.jar:lib/hanoi.jar Player -w

