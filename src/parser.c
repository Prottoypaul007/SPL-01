#include <stdio.h>
#include <stdlib.h>
#include "../include/parser.h"
#include "../include/matrix.h"

int** parseInput(char* filename, int* N) {
    FILE* file = fopen(filename, "r");
    if (file == NULL) {
        printf("[Error] Could not open file: %s\n", filename);
        return NULL;
    }

    if (fscanf(file, "%d", N) != 1) {
        printf("[Error] Invalid file format (missing N).\n");
        fclose(file);
        return NULL;
    }

    int** matrix = createMatrix(*N); 

    for (int i = 0; i < *N; i++) {
        for (int j = 0; j < *N; j++) {
            if (fscanf(file, "%d", &matrix[i][j]) != 1) {
                printf("[Error] Invalid matrix data.\n");
                fclose(file);
                return NULL;
            }
        }
    }

    fclose(file);
    return matrix;
}