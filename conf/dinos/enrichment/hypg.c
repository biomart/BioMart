#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <math.h>
#include <unistd.h>

#define MAX 2000
#define UNDEF "NA"

void usage(void) {
    fprintf(stderr, "Usage hypg -g genome_file -a annotation_file -c cutoff");
    fprintf(stderr, " -s set_file\n");
    fprintf(stderr, "  [-p prefix]\n");
    fprintf(stderr, "  [-m maxsize]\n");
    fprintf(stderr, "  [-S minimum prevalence for listing (default 0)]\n");
    fprintf(stderr, "  [-b test for overrepresentation or underrepresentation depending on\n");
    fprintf(stderr, "      result of comparison with expected\n");
    fprintf(stderr, "  [-B use Bonferroni corrected P-values to compare with cutoff]\n");
    fprintf(stderr, "  [-i to be used when sets and annotations are the same (does not test self-overlap and\n");
    fprintf(stderr, "      compares each pair only once)]\n");
    return;
}

void addgene(const char *name);
void addanno(const char *gene, const char *anno);
void analyze(int set_S, int set_b, int set_B, int set_i);
void rmfirst(char *new, const char *old);
void usage(void);
double hypg(int N, int n, int M, int m);
double hypg_low(int N, int n, int M, int m);
int find(const char *name, char **list, int n);
double bonf(double pv, int ntest);
void nline(FILE *f);

char **gene_name = NULL, **anno_term = NULL;

int ngene = 0, nanno = 0, nset = 0, ntotset = 0, ntotanno = 0, ntest = 0;
char genfile[MAX], annfile[MAX], setfile[MAX], line[MAX], set[MAX];
char prefix[MAX], pv_file[MAX], list_file[MAX], report_file[MAX];
char check[MAX];
int *used_ann = NULL;
int *n_anno;
int **anno_list;
int *gen_prev = NULL, *set_prev = NULL;
double cutoff;
double *cutoff_sz = NULL;
int maxsize, minprev;
int *def_cut = NULL;
int **gene_list;
FILE *fout_pv, *fout_list, *fout_report;


int main(int argc, char *argv[]) {
    int i, j, labgen;
    int *flag;
    char gname[MAX], anno[MAX];
    char report[MAX][MAX];
    FILE *fgen, *fann, *fset;
    int c;
    int set_g = 0, set_a = 0, set_s = 0, set_c = 0, set_p = 0, set_h = 0;
    int set_m = 0, set_S = 0, set_b = 0, set_B = 0, set_i = 0;

    extern char *optarg;
    extern int optind, opterr, optopt;

    // input

    strcpy(prefix, "hypg");

    minprev = 0;
    while (1) {
        c = getopt(argc, argv, "hS:g:a:s:c:p:m:bBi");
        if (c == -1)
            break;
        switch (c) {
        case 'h':
            set_h = 1;
            break;
        case 'g':
            strcpy(genfile, optarg);
            set_g = 1;
            break;
        case 'a':
            strcpy(annfile, optarg);
            set_a = 1;
            break;
        case 's':
            strcpy(setfile, optarg);
            set_s = 1;
            break;
        case 'c':
            cutoff = atof(optarg);
            set_c = 1;
            break;
        case 'm':
            maxsize = atoi(optarg);
            set_m = 1;
            break;
        case 'p':
            strcpy(prefix, optarg);
            set_p = 1;
            break;
        case 'S':
            set_S = 1;
            minprev = atoi(optarg);
            break;
        case 'b':
            set_b = 1;
            break;
        case 'B':
            set_B = 1;
            break;
        case 'i':
            set_i = 1;
            break;
        case '?':
            break;
        default:
            break;
        }
    }

    // check input


    if (set_h) {
        usage();
        exit(EXIT_SUCCESS);
    }

    if (!(set_g * set_a * set_s * set_c)) {
        usage();
        exit(EXIT_FAILURE);
    }
    // report input


    // read genome
    fgen = fopen(genfile, "r");

    if (fgen == NULL) {
        fprintf(stderr, "Error opening %s\n", genfile);
        exit(EXIT_FAILURE);
    }

    while (fscanf(fgen, "%s", gname) != EOF) {
        addgene(gname);
    }
    fclose(fgen);

    n_anno = (int *) malloc(ngene * sizeof(int));
    for (i = 0; i < ngene; ++i) {
        n_anno[i] = 0;
    }
    anno_list = (int **) malloc(ngene * sizeof(int *));
    for (i = 0; i < ngene; ++i) {
        anno_list[i] = NULL;
    }
    if (!set_m) {
        maxsize = ngene;
    }

    // read annotation
    fann = fopen(annfile, "r");
    if (fann == NULL) {
        fprintf(stderr, "Error opening %s\n", annfile);
        exit(EXIT_FAILURE);
    }

    while (fscanf(fann, "%s %s", gname, anno) != EOF) {
        addanno(gname, anno);
    }
    fclose(fann);

    // compute the number of used annotations (for multiple testing)
    ntotanno = 0;
    for (i = 0; i < nanno; ++i) {
        if (used_ann[i] && gen_prev[i] <= maxsize) {
            ++ntotanno;
        }
    }

    // open output files

    strcpy(pv_file, prefix);
    strcat(pv_file, ".pv");
    strcpy(list_file, prefix);
    strcat(list_file, ".list");
    strcpy(report_file, prefix);
    strcat(report_file, ".rep");
    fout_pv = fopen(pv_file, "w");
    fout_list = fopen(list_file, "w");
    fout_report = fopen(report_file, "w");

    // read and analyze sets
    set_prev = (int *) malloc(nanno * sizeof(int));
    gene_list = (int **) malloc(nanno * sizeof(int *));
    flag = (int *) malloc(nanno * sizeof(int));

    fset = fopen(setfile, "r");
    if (fset == NULL) {
        fprintf(stderr, "Error opening %s\n", setfile);
        exit(EXIT_FAILURE);
    }

    // count number of sets (for multiple testing)
    ntotset = 0;
    while (fscanf(fset, "%s", line) != EOF) {
        if (line[0] == '>') {
            ++ntotset;
        }
    }
    ntest = ntotanno * ntotset;

    // correct ntest when sets and annotations are identical
    if (set_i) {
        if (ntotset != ntotanno) {
            fprintf(stderr, "Warning: option -i assumes sets and annotations to be identical\n");
            fprintf(stderr, "but here we have %d sets and %d used annotations \n", ntotset, ntotanno);
        }
        ntest = ntotanno * (ntotanno - 1) / 2;
    }

    rewind(fset);

    fprintf(fout_report, "annotations:\t%d\n", ntotanno);
    fprintf(fout_report, "sets:\t%d\n", ntotset);
    fprintf(fout_report, "no_of_tests:\t%d\n", ntest);

    while (fscanf(fset, "%s", line) != EOF) {
        if (line[0] == '>') {
            rmfirst(set, line);
            nset = 0;
            for (i = 0; i < nanno; ++i) {
                set_prev[i] = 0;
                gene_list[i] = NULL;
                flag[i] = 0;
            }
        }
        else if (line[0] == '<') {
            rmfirst(check, line);
            if (strcmp(check, set)) {
                fprintf(stderr, "Error: set %s terminated by <%s\n", set, check);
                exit(EXIT_FAILURE);
            }
            analyze(set_S, set_b, set_B, set_i);
            for (i = 0; i < nanno; ++i) {
                if (flag[i]) {
                    free(gene_list[i]);
                }
            }
        }
        else {
            labgen = find(line, gene_name, ngene);
            if (labgen == -1) {
                continue;
            }
            ++nset;
            for (i = 0; i < n_anno[labgen]; ++i) {
                j = anno_list[labgen][i];
                ++set_prev[j];
                gene_list[j] = (int *) realloc(gene_list[j], set_prev[j]
                                               * sizeof(int));
                flag[j] = 1;
                gene_list[j][set_prev[j] - 1] = labgen;
            }
        }
    }
    fclose(fout_pv);
    fclose(fout_list);
    fclose(fout_report);
    return 0;
}

void addgene(const char *name) {
    int label;

    label = find(name, gene_name, ngene);

    if (label != -1) {
        fprintf(stderr, "Warning: multiple instances of %s in genome file %s\n", name, genfile);
        return;
    }

    else {
        ++ngene;
        gene_name = (char **) realloc(gene_name, ngene * sizeof(char *));
        gene_name[ngene - 1] = (char *) malloc((strlen(name) + 1) * sizeof(char));
        strcpy(gene_name[ngene - 1], name);
    }
}

void addanno(const char *gene, const char *anno) {

    int labann, labgen, i, is_new;

    // find the label of this annotation or assign a new label if new
    labann = find(anno, anno_term, nanno);
    if (labann == -1) {
        is_new = 1;
        ++nanno;
        used_ann = (int *) realloc(used_ann, nanno * sizeof(int));
        anno_term = (char **) realloc(anno_term, nanno * sizeof(char *));
        anno_term[nanno - 1] = (char *) malloc((strlen(anno) + 1) * sizeof(char));
        strcpy(anno_term[nanno - 1], anno);
        gen_prev = (int *) realloc(gen_prev, nanno * sizeof(int));
        gen_prev[nanno - 1] = 0;
        labann = nanno - 1;
        used_ann[labann] = 0;
    }

    // find the label of the gene or discard if the gene is not in the genome
    labgen = find(gene, gene_name, ngene);
    if (labgen == -1) {
        return;  // not counted if gene is not in genome
    }

    // check whether this annotation had been previously assigned to this gene
    for (i = 0; i < n_anno[labgen]; ++i) {
        if (anno_list[labgen][i] == labann) {
            fprintf(stderr, "Warning: multiple instances of association %s - %s", gene, anno);
            fprintf(stderr, " in annotation file %s\n", annfile);
            return;
        }
    }

    // add the annotation to the gene
    used_ann[labann] = 1;
    ++n_anno[labgen];
    anno_list[labgen] = (int *) realloc(anno_list[labgen], n_anno[labgen] * sizeof(int));
    anno_list[labgen][n_anno[labgen] - 1] = labann;
    ++gen_prev[labann];
}


int find(const char *name, char **list, int n) {
    int i;

    for (i = 0; i < n; ++i) {
        if (strcmp(name, list[i]) == 0)
            return i;
    }
    return -1;
}

void rmfirst(char *new, const char *old) {
    int i;

    for (i = 1; i <= strlen(old); ++i) {
        new[i - 1] = old[i];
    }
}

void analyze(int set_S, int set_b, int set_B, int set_i) {
    int i, j;
    double pv, expect, pvcut, adjpv;
    char sign;

    pvcut = cutoff;

    for (i = 0; i < nanno; ++i) {

        if ((set_S && set_prev[i] < minprev) || (gen_prev[i] > maxsize)) continue;
        if (set_i && strcmp(anno_term[i], set) <= 0) continue;
        expect = (double) (gen_prev[i] * nset) / (double) ngene;

        if (set_prev[i] >= expect) {
            sign = '+';
        }
        else {
            sign = '-';
        }

        if (! set_b) {
            pv = hypg(ngene, gen_prev[i], nset, set_prev[i]);
            adjpv = bonf(pv, ntest);
            if (adjpv <= cutoff || ! set_B && pv <= cutoff) {
                // fprintf(fout_pv, "%s\t%s\t%d\t%d\t%d\t%d\t%g\t%c\t%g\t%g", set, anno_term[i], ngene, nset, gen_prev[i], set_prev[i],
                //         expect, sign, pv, adjpv);
                // fprintf(fout_pv, "\n");
                // for (j = 0; j < set_prev[i]; ++j) {
                //     fprintf(fout_list, "%s\t%s\t%s\n", set, anno_term[i], gene_name[gene_list[i][j]]);
                // }

                // New format:
                fprintf(fout_pv, "%s\t%g\t%g", anno_term[i], pv, adjpv);
                if (set_prev[i]) fprintf(fout_pv, "\t");
                for (j = 0; j < set_prev[i]; ++j) {
                    fprintf(fout_pv, "%s", gene_name[gene_list[i][j]]);
                    if (j < set_prev[i] - 1) fprintf(fout_pv, "\t");
                }
                fprintf(fout_pv, "\n");
            }
        }

        else {
            if (sign == '+') {
                pv = hypg(ngene, gen_prev[i], nset, set_prev[i]);
            }
            else {
                pv = hypg_low(ngene, gen_prev[i], nset, set_prev[i]);
            }
            adjpv = bonf(pv, ntest);
            if (adjpv <= cutoff || ! set_B && pv <= cutoff) {
                // fprintf(fout_pv, "%s\t%s\t%d\t%d\t%d\t%d\t%g\t%c\t%g\t%g", set, anno_term[i], ngene, nset, gen_prev[i], set_prev[i],
                //         expect, sign, pv, adjpv);
                // fprintf(fout_pv, "\n");
                // for (j = 0; j < set_prev[i]; ++j) {
                //     fprintf(fout_list, "%s\t%s\t%s\n", set, anno_term[i], gene_name[gene_list[i][j]]);
                // }

                // New format:
                fprintf(fout_pv, "%s\t%g\t%g", anno_term[i], pv, adjpv);
                if (set_prev[i]) fprintf(fout_pv, "\t");
                for (j = 0; j < set_prev[i]; ++j) {
                    fprintf(fout_pv, "%s", gene_name[gene_list[i][j]]);
                    if (j < set_prev[i] - 1) fprintf(fout_pv, "\t");
                }
                fprintf(fout_pv, "\n");
            }
        }

    }

}



double gammaln(float xx) {
    // Returns the value ln[Gamma(xx)] for xx > 0.

    if (xx > 0) {
        double x, y, tmp, ser;
        static double cof[6] = { 76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5 };
        int j;

        y = x = xx;
        tmp = x + 5.5;
        tmp -= (x + 0.5) * log(tmp);
        ser = 1.000000000190015;
        for (j = 0; j <= 5; j++)
            ser += cof[j] / ++y;
        return (-tmp + log(2.5066282746310005 * ser / x));
    } else {
        return 1.0f;
    }
}

double bicoln(int n, int k)
{

    // Returns the log of binomial coefficient ( n k ) as a floating-point number.

    return gammaln(n + 1) - gammaln(k + 1) - gammaln(n - k + 1);

}


double hypg(int N, int n, int M, int m)
{
    int k, max;
    double tmp_pvalue = 0;

    max = n;
    if (M < n) {
        max = M;
    }

    for (k = m; k <= max; k++) {
        tmp_pvalue += exp(bicoln(N - n, M - k) + bicoln(n, k) - bicoln(N, M));
    }
    return tmp_pvalue;
}

double hypg_low(int N, int n, int M, int m)
{
    double tmp_pvalue = 0;
    int min, k;

    min = 0;
    if (M > N - n) {
        min = M - (N - n);
    }


    for (k = min; k <= m; ++k) {
        tmp_pvalue += exp(bicoln(N - n, M - k) + bicoln(n, k) - bicoln(N, M));
    }

    return tmp_pvalue;
}

double bonf(double pv, int ntest)
{
    if (ntest * pv < 1) {
        return (double) ntest * pv;
    } else {
        return (double) 1;
    }
}

void nline(FILE *f)
{
    while (getc(f) != '\n');
}
