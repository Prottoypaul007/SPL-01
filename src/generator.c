#include "../include/generator.h"
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

void generateRandomTest(int N, const char* filename, int isSymmetric) {
    FILE* file = fopen(filename, "w");
    if (file == NULL) {
        fprintf(stderr, "Error: Could not write to file '%s'\n", filename); //stderr=a pre-defined output stream used specifically for outputting error messages and diagnostics
        return;
    }

    fprintf(file, "%d\n", N);

    srand(time(NULL));

    int** mat = (int**)malloc(N * sizeof(int*));
    for(int i=0; i<N; i++) mat[i] = (int*)malloc(N * sizeof(int)); //creating 2d space

    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            if (i == j) {
                mat[i][j] = 0; 
            } else {
                int weight = (rand() % 100) + 1; 
                mat[i][j] = weight;
            }
        }
    }

    if (isSymmetric) {
        for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
                mat[j][i] = mat[i][j];
            }
        }
    }

    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            fprintf(file, "%d ", mat[i][j]);
        }
        fprintf(file, "\n");
    }
    for(int i=0; i<N; i++) free(mat[i]);
    free(mat);
    fclose(file);

    printf("Generated random test case '%s' with %d cities.\n", filename, N);
}