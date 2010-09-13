#TEXINPUTS=.:`pwd`/figs:
PAPER=aamas11storage
SECTIONS := $(wildcard sections/*.tex)
FIGS := $(wildcard figs/*.*)
DATA := $(wildcard data**/*.*data)
all: ${PAPER}.pdf test

${PAPER}.pdf: ${PAPER}.tex ${PAPER}.bib ${SECTIONS} ${FIGS} ${DATA}
	pdflatex ${PAPER}
	bibtex ${PAPER}
	pdflatex ${PAPER}
	pdflatex ${PAPER}

test: ${PAPER}.pdf
	pdffonts -cfg /dev/null ${PAPER}.pdf
clean:
	rm -f ${PAPER}.aux
	rm -f ${PAPER}.blg
	rm -f ${PAPER}.bbl
	rm -f ${PAPER}.dvi
	rm -f ${PAPER}.ps
	rm -f ${PAPER}.pfg
	rm -f ${PAPER}.log

clean-all: clean
	rm -f ${PAPER}.pdf
