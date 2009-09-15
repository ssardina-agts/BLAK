TEXINPUTS=.:./styles::

PAPER=aamas10bdilearning
SECTIONS := $(wildcard sections/*.tex)
FIGS := $(wildcard figs/*.*)

all: ${PAPER}.pdf test

${PAPER}.pdf: ${PAPER}.tex ${PAPER}.bib ${SECTIONS} ${FIGS}
	latex ${PAPER}
	bibtex ${PAPER}
	latex ${PAPER}
	latex ${PAPER}
	dvips -Ppdf -G0 -tletter ${PAPER}.dvi
	ps2pdf -dPDFSETTINGS=/printer \
		-dCompatibilityLevel=1.4 -dMaxSubsetPct=0 \
		-dSubsetFonts=false -dEmbedAllFonts=true \
		-sPAPERSIZE=letter ${PAPER}.ps 

test: ${PAPER}.pdf
	pdffonts -cfg /dev/null ${PAPER}.pdf
clean:
	rm -f ${PAPER}.aux
	rm -f ${PAPER}.blg
	rm -f ${PAPER}.bbl
	rm -f ${PAPER}.dvi
	rm -f ${PAPER}.ps
	rm -f ${PAPER}.log

clean-all: clean
	rm -f ${PAPER}.pdf
