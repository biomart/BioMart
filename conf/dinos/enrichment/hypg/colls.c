#include "colls.h"


void bm_gene_init(struct gene *g) {
    g->number = 0;
    g->genes = NULL;
}

void bm_gene_create(struct gene *g, size_t size) {
    g->genes = (char **) malloc(size * sizeof(char*));
}

void bm_gene_free(struct gene *g) {
    for (int i = 0, ii = g->number; i < ii; ++i) { free(g->genes[i]); }
    free(g->genes);
    bm_gene_init(g);
}

void bm_gene_add_gene(struct gene *g, const char* gene_name) {
    g->genes[g->number] = (char *) malloc((strlen(gene_name) + 1) * sizeof(char));
    strcpy(g->genes[g->number], gene_name);
    ++g->number;
}

size_t bm_gene_count(FILE *f) {
  size_t n = 0;
  char gene[256];

  while (fscanf(f, "%s", gene) != EOF) { ++n; }

  return n;
}

void bm_add_gene(struct gene *genes, const char *name) {
    int idx = bm_find_index(genes->genes, name, genes->number);

    if (idx != -1) {
        fprintf(stderr, "Warning: multiple instances of %s in genome file\n", name);
    } else {
        bm_gene_add_gene(genes, name);
    }
}



// int bm_cmp (void const* a, void const* b) {
//     char const **aa = NULL, **bb = NULL;
//     if (a != NULL) {
//         aa = (char const**) a;
//         printf("BM - bm_cmp(%s,", *aa);
//     }else {
//         printf("BM - bm_cmp(NULL,");
//     }
//     if (b != NULL) {
//         bb = (char const**) b;
//         printf("%s)\n", *bb);
//     }else {
//         printf("NULL)\n");
//     }

//     return aa != NULL && bb != NULL ? strcmp(*aa, *bb) : 0;
// }

int bm_cmp (void const* a, void const* b) {
    char const **aa = (char const**) a, **bb = (char const**) b;
    return strcmp(*aa, *bb);
}

const char** bm_find(const char **list, const char *key, const size_t n) {
    void *p = bsearch(&key, list, n, sizeof(*list), bm_cmp);
    return p;
}

int bm_find_index(const char **list, const char *key, const size_t n) {
    const char **el = bm_find(list, key, n);
    return el == NULL ? -1 : (el - list);
}

void bm_sort(char **coll, size_t size) {
    qsort(coll, size, sizeof(char*), bm_cmp);
}










void bm_anno_init(struct anno *a) {
    a->number = 0;
    a->annos = NULL;
}

void bm_anno_create(struct anno *g) {
    g->annos = (char **) malloc(g->number * sizeof(char*));
}

void bm_anno_free(struct anno *a) {
    for (int i = 0, ii = a->number; i < ii; ++i) { free(a->annos + i); }
    free(a->annos);
}
