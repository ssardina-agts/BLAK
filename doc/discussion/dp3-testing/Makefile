TEXINPUTS=.:./styles::

PAPER=dp3-testing

all: ${PAPER}.pdf

${PAPER}.pdf: ${PAPER}.tex 
	pdflatex ${PAPER}
	#bibtex ${PAPER}
	#pdflatex ${PAPER}
	pdflatex ${PAPER}

test: ${PAPER}.pdf
	pdffonts -cfg /dev/null ${PAPER}.pdf
clean:
	rm -f ${PAPER}.{aux,toc,blg,bbl,dvi,ps,log}

clean-all: clean
	rm -f ${PAPER}.pdf
