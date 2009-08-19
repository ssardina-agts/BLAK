TEXINPUTS=.:./styles::

PAPER=dp1-new-worlds
RESULTS := $(wildcard blak*.pdf)

all: ${PAPER}.pdf

${PAPER}.pdf: ${PAPER}.tex biblio.bib ${RESULTS}
	pdflatex ${PAPER}
	bibtex ${PAPER}
	pdflatex ${PAPER}
	pdflatex ${PAPER}

test: ${PAPER}.pdf
	pdffonts -cfg /dev/null ${PAPER}.pdf
clean:
	rm -f ${PAPER}.{aux,toc,blg,bbl,dvi,ps,log}

clean-all: clean
	rm -f ${PAPER}.pdf
