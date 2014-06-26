CXX=gcc -std=c99
DATADIR=./data
CFLAGS= -c -O3 -w
DEFBK=./data/hsapiens_gene_ensembl_default_background
SET=./data/sets503036268651660146filter
ANNS=./data/hsapiens_gene_ensemblgene_ensembl_config_3_1_2goslim_goa_accession
CUTOFF=0.05

hypg.run: hypg.o colls.o
	$(CXX) -lm $^ -o $@

hypg.o: hypg.c colls.o
	$(CXX) $(CFLAGS) $< -o $@

colls.o: colls.c colls.h
	$(CXX) $(CFLAGS) $< -o $@

.PHONY: run t clean run_orig diff

t: test.run
	./test.run

test.run: test.o colls.o
	$(CXX) $^ -o $@

test.o: test.c
	$(CXX) $(CFLAGS) $< -o $@

diff:
	diff hypg.pv.bm hypg.pv > hypg.diff

run_orig:
	./original/hypg -g $(DEFBK) -s $(SET) -a $(ANNS) -c $(CUTOFF)

run:
	./hypg.run -g $(DEFBK) -s $(SET) -a $(ANNS) -c $(CUTOFF) > hypg.pv.bm

clean:
	rm -f *.o *.run *.pv *.list *.rep *.pv.bm *.diff