
#include <assert.h>
#include <string.h>
#include "colls.h"


void create_genes(gene *g, const char** genes, size_t len) {
  bm_gene_init(g);
  bm_gene_create(g, len);
  int i = -1;
  while(++i < len) {
    bm_gene_add_gene(g, genes[i]);
  }
}

void init_gene_test() {
  gene g;
  
  bm_gene_init(&g);

  assert(g.number == 0);
  assert(g.genes == NULL);

  bm_gene_create(&g, 2);

  assert(g.number == 0);
  assert(g.genes != NULL);
  bm_gene_free(&g);
}

void add_gene_test() {
  gene g;
  const char *genes[] = {
    "RARA", "TSP", "BAR", "FOO"
  };

  create_genes(&g, genes, 4);
  assert(g.number == 4);
  assert(strcmp(g.genes[0], genes[0]) == 0);
  assert(strcmp(g.genes[1], genes[1]) == 0);
  assert(strcmp(g.genes[2], genes[2]) == 0);
  assert(strcmp(g.genes[3], genes[3]) == 0);

  bm_gene_free(&g);
  assert(g.number == 0);
  assert(g.genes == NULL);
  bm_gene_free(&g);
}


void sort_test() {
  gene g;
  const char *genes[] = {
      "RARA", "TSP", "BAR", "FOO"
    },
    *sorted_genes[] = {
      "BAR", "FOO", "RARA", "TSP"
    };

  create_genes(&g, genes, 4);
  bm_sort(g.genes, 4);

  for (int i = 0; i < 4; ++i) {
    assert(strcmp(g.genes[i], sorted_genes[i]) == 0);
  }
  bm_gene_free(&g);
}


void find_index_test() {
  gene g;
  const char *genes[] = {
      "BAR", "FOO", "RARA", "TSP"
    };

  create_genes(&g, genes, 4);
  
  int idx = -1;
  for (size_t i = 0; i < 4; ++i) {
    idx = bm_find_index(g.genes, genes[i], g.number);
    assert(idx == i);
  }
  bm_gene_free(&g);
}



int main() {

  init_gene_test();
  add_gene_test();
  sort_test();
  find_index_test();

	return 0;
}



