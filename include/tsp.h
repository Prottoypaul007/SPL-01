#ifndef TSP_H
#define TSP_H

#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

#define INF INT_MAX
typedef struct Node {
    int vertex;             
    int level;              
    int** reducedMatrix;    
    int cost;               
    int* path;             
} Node;

Node* newNode(int** parentMatrix, int* path, int level, int i, int j, int N);
int calculateCost(int** reducedMatrix, int N, int u, int v);
void printPath(int* path, int N, int cost);

void solveTSP(int** costMatrix, int N, int startNode);

#endif