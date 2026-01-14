#include "../include/tsp.h"
#include "../include/matrix.h"
#include "../include/heap.h"
#include <stdio.h>
#include <stdlib.h>
Node* newNode(int** parentMatrix, int* parentPath, int level, int i, int j, int N) {
    Node* node = (Node*)malloc(sizeof(Node));
    if (node == NULL) {
        printf("\n[Critical Error] Out of Memory: Could not allocate new Node.\n");
        printf("The problem size (N=%d) requires more RAM than available.\n", N);
        exit(1);
    }

    node->path = (int*)malloc((N + 1) * sizeof(int));
    if (node->path == NULL) {
        printf("[Critical Error] Out of Memory for Path array.\n");
        exit(1);
    }
    
    if (level != 0) {
        for (int k = 0; k < level; k++) {
            node->path[k] = parentPath[k];
        }
    }
    node->path[level] = j; 

    node->reducedMatrix = createMatrix(N); 
    
    copyMatrix(parentMatrix, node->reducedMatrix, N);

    if (level != 0) {
        for (int k = 0; k < N; k++) {
            node->reducedMatrix[i][k] = INF; 
            node->reducedMatrix[k][j] = INF; 
        }
        node->reducedMatrix[j][node->path[0]] = INF; 
    }

    node->level = level;
    node->vertex = j;

    return node;
}

int calculateCost(int** reducedMatrix, int N, int u, int v) {
    int* rowRed = (int*)malloc(N * sizeof(int));
    int* colRed = (int*)malloc(N * sizeof(int));
    
    if (!rowRed || !colRed) {
         printf("[Error] Memory allocation failed during cost calculation.\n");
         exit(1);
    }

    int rCost = rowReduction(reducedMatrix, N, rowRed);
    int cCost = columnReduction(reducedMatrix, N, colRed);

    free(rowRed);
    free(colRed);

    return rCost + cCost;
}

void printPath(int* path, int N, int cost) {
    printf("\n TSP Solution done \n");
    printf("Minimum Cost: %d\n", cost);
    printf("Path: ");
    for (int i = 0; i < N; i++) {
        printf("%d --- ", path[i]);
    }
    printf("%d\n", path[0]); 

    FILE* f = fopen("solution.csv", "w");
    if (f != NULL) {
        fprintf(f, "%d,", cost); 
        for (int i = 0; i < N; i++) {
            fprintf(f, "%d", path[i]);
            if (i < N - 1) fprintf(f, ",");
        }
        fprintf(f, ",%d", path[0]); 
        fclose(f);
    }
}

void solveTSP(int** costMatrix, int N, int startNode) {
    
    if (N < 2) {
        printf("Trivial solution: Distance is 0.\n");
        FILE* f = fopen("solution.csv", "w");
        fprintf(f, "0,%d,%d", startNode, startNode);
        fclose(f);
        return;
    }

    MinHeap* pq = createMinHeap(1200000); 

    int* emptyPath = (int*)malloc((N + 1) * sizeof(int));
    
    Node* root = newNode(costMatrix, emptyPath, 0, -1, startNode, N);
    root->cost = calculateCost(root->reducedMatrix, N, -1, startNode);

    push(pq, root);

    printf("[Backend] Solver initialized.\n");

    while (!isEmpty(pq)) {
        Node* min = pop(pq);
        int i = min->vertex;

        if (min->level == N - 1) {
            printPath(min->path, N, min->cost);
            
            free(pq->array);
            free(pq);
            return; 
        }

        for (int j = 0; j < N; j++) {
            if (min->reducedMatrix[i][j] != INF) {
                Node* child = newNode(min->reducedMatrix, min->path, min->level + 1, i, j, N);
                int reductionCost = calculateCost(child->reducedMatrix, N, i, j);
                child->cost = min->cost + min->reducedMatrix[i][j] + reductionCost;
                push(pq, child);
            }
        }

        destroyMatrix(min->reducedMatrix, N);
        free(min->path);
        free(min);
    }
        printf("\n[Alert] No valid Hamiltonian Cycle exists.\n");
    FILE* f = fopen("solution.csv", "w");
    if (f != NULL) { fprintf(f, "-1"); fclose(f); }
}