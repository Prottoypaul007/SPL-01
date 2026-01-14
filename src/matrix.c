#include "../include/matrix.h"
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

int** createMatrix(int N) {
    int** matrix = (int**)malloc(N * sizeof(int*));
    if (matrix == NULL) {
        printf("\n Memory allocation failed for matrix rows (N=%d).\n", N);
        exit(1);
    }

    for (int i = 0; i < N; i++) {
        matrix[i] = (int*)malloc(N * sizeof(int));
        if (matrix[i] == NULL) {
            printf("\n Memory allocation failed for matrix column %d.\n", i);
            exit(1); 
        }
    }
    return matrix;
}

void copyMatrix(int** src, int** dest, int N) {
    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            dest[i][j] = src[i][j];
        }
    }
}

void destroyMatrix(int** matrix, int N) {
    if (matrix == NULL) return;
    for (int i = 0; i < N; i++) {
        if (matrix[i] != NULL) free(matrix[i]);
    }
    free(matrix);
}

int rowReduction(int** matrix, int N, int* rowRed) {
    int cost = 0;
    for (int i = 0; i < N; i++) {
        int min = INT_MAX;
        for (int j = 0; j < N; j++) {
            if (matrix[i][j] < min)
                min = matrix[i][j];
        }
        if (min != INT_MAX && min != 0) {
            for (int j = 0; j < N; j++) {
                if (matrix[i][j] != INT_MAX)
                    matrix[i][j] -= min;
            }
            cost =cost+ min;
        }
    }
    return cost;
}

int columnReduction(int** matrix, int N, int* colRed) {
    int cost = 0;
    for (int j = 0; j < N; j++) {
        int min = INT_MAX;
        for (int i = 0; i < N; i++) {
            if (matrix[i][j] < min)
                min = matrix[i][j];
        }
        if (min != INT_MAX && min != 0) {
            for (int i = 0; i < N; i++) {
                if (matrix[i][j] != INT_MAX)
                    matrix[i][j] -= min;
            }
            cost =cost+ min;
        }
    }
    return cost;
}