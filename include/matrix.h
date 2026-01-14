#ifndef MATRIX_H
#define MATRIX_H

#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

#define INF INT_MAX

int** createMatrix(int N);

void destroyMatrix(int** matrix, int N);

void copyMatrix(int** source, int** dest, int N);

int rowReduction(int** matrix, int N, int* rowReductions);

int columnReduction(int** matrix, int N, int* colReductions);

void printMatrix(int** matrix, int N);

#endif