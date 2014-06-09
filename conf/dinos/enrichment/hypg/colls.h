
#ifndef __HYPG_H
#define __HYPG_H 1

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

typedef struct gene {
    size_t number;
    char **genes; /* gene_name */
} gene;

typedef struct anno {
    size_t number;
    char **annos; /* anno_term */
} anno;

/**
 * initializes the state to the zero of each type
 */
void bm_gene_init(struct gene *g);

/**
 * instantiates the collections
 */
void bm_gene_create(struct gene *g, size_t size);

void bm_gene_free(struct gene *g);

void bm_gene_add_gene(struct gene *g, const char* gene_name);

size_t bm_gene_count(FILE *f);



void bm_anno_init(struct anno *a);

void bm_anno_create(struct anno *g);

void bm_anno_free(struct anno *g);



int bm_cmp (void const* a, void const* b);

const char** bm_find(const char **list, const char *key, const size_t n);

int bm_find_index(const char **list, const char *key, const size_t n);

void bm_sort(char **coll, size_t size);

#endif