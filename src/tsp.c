#include "../include/tsp.h"
#include "../include/matrix.h"
#include "../include/heap.h"
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

Node* newNode(int** parentMatrix, int* parentPath,
              int level, int from, int to, int N) {
    // mmry allocation for nd
    Node* node = (Node*)malloc(sizeof(Node));
    if (!node) {
        printf("[Error] Cannot allocate memory for Node\n");
        exit(0);
    }
    // mmry for path
    node->path = (int*)malloc((N + 1) * sizeof(int));
    if (!node->path) {
        printf("[Error] Cannot allocate memory for Path\n");
        free(node);
        exit(0);
    }
    // Copy prnt path
    if (level != 0) {
        for (int k = 0; k < level; k++) {
            node->path[k] = parentPath[k];
        }
    }
    //add currnt path
    node->path[level] = to;

    //reduced matrix
    node->reducedMatrix = createMatrix(N);
    if (node->reducedMatrix == NULL) {
        printf("\n[Safety Stop] Memory limit reached.\n");
        printf("Stopping exact solver.\n");
        exit(0);
    }
    // Copy parent matrix into child
    copyMatrix(parentMatrix, node->reducedMatrix, N);
    if (level != 0) {
        for (int k = 0; k < N; k++) {
            node->reducedMatrix[from][k] = INF;
            node->reducedMatrix[k][to]   = INF;
        }
        node->reducedMatrix[to][node->path[0]] = INF;
    }

    node->level  = level;
    node->vertex = to;

    return node;
}
//lower bound cost of a node
int calculateCost(int** matrix, int N, int u, int v) {

    int* rowReductionArray = (int*)malloc(N * sizeof(int));
    int* colReductionArray = (int*)malloc(N * sizeof(int));

    if (!rowReductionArray || !colReductionArray) {
        return INF;
    }

    int rowCost = rowReduction(matrix, N, rowReductionArray);
    int colCost = columnReduction(matrix, N, colReductionArray);

    free(rowReductionArray);
    free(colReductionArray);

    return rowCost + colCost;
}
void printPath(int* path, int N, int cost) {

    printf("\n[B&B] Optimal Solution Found:\n");
    printf("Minimum Cost: %d\n", cost);

    printf("Path: ");
    for (int i = 0; i < N; i++) {
        printf("%d -> ", path[i]);
    }
    printf("%d\n", path[0]);
    FILE* file = fopen("solution.csv", "w");
    if (file != NULL) {
        fprintf(file, "%d,", cost);

        for (int i = 0; i < N; i++) {
            fprintf(file, "%d", path[i]);
            if (i < N - 1) fprintf(file, ",");
        }

        fprintf(file, ",%d", path[0]);
        fclose(file);
    }
}

void solveTSP(int** costMatrix, int N,
              int startNode, int upperBound) {
    if (N < 2) {
        FILE* f = fopen("solution.csv", "w");
        if (f) {
            fprintf(f, "0,%d,%d", startNode, startNode);
            fclose(f);
        }
        return;
    }
    MinHeap* pq = createMinHeap(2000000);

    int* tempPath = (int*)malloc((N + 1) * sizeof(int));
    Node* root = newNode(costMatrix, tempPath,
                         0, -1, startNode, N);

    root->cost = calculateCost(root->reducedMatrix,
                               N, -1, startNode);

    push(pq, root);

    printf("[Backend] Solver started. Limit: %d\n", upperBound);

    while (!isEmpty(pq)) {

        Node* current = pop(pq);
        // Prune 
        if (current->cost >= upperBound) {
            destroyMatrix(current->reducedMatrix, N);
            free(current->path);
            free(current);
            continue;
        }
        // jodi full path reached
        if (current->level == N - 1) {
            printPath(current->path, N, current->cost);
            free(pq->array);
            free(pq);
            return;
        }
        for (int next = 0; next < N; next++) {

            if (current->reducedMatrix[current->vertex][next] != INF) {

                Node* child = newNode(
                    current->reducedMatrix,
                    current->path,
                    current->level + 1,
                    current->vertex,
                    next,
                    N
                );

                int reductionCost = calculateCost(
                    child->reducedMatrix,
                    N,
                    current->vertex,
                    next
                );

                child->cost = current->cost
                            + current->reducedMatrix[current->vertex][next]
                            + reductionCost;
                if (child->cost < upperBound) {
                    push(pq, child);
                } else {
                    destroyMatrix(child->reducedMatrix, N);
                    free(child->path);
                    free(child);
                }
            }
        }
        destroyMatrix(current->reducedMatrix, N);
        free(current->path);
        free(current);
    }
    printf("[Backend] No better solution than %d found.\n", upperBound);
}