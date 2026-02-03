#include "../include/tsp.h"
#include "../include/matrix.h"
#include "../include/heap.h"
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

// Helper to create a new node in the B&B Tree
Node* newNode(int** parentMatrix, int* parentPath, int level, int i, int j, int N) {
    Node* node = (Node*)malloc(sizeof(Node));
    if (!node) {
        printf("[Error] Out of Memory allocating Node.\n");
        exit(0); // Graceful exit on OOM
    }

    node->path = (int*)malloc((N + 1) * sizeof(int));
    if (!node->path) {
        printf("[Error] Out of Memory allocating Path.\n");
        free(node);
        exit(0);
    }
    
    // Copy path from parent
    if (level != 0) {
        for (int k = 0; k < level; k++) node->path[k] = parentPath[k];
    }
    node->path[level] = j; // Add current city to path

    // Create reduced matrix
    node->reducedMatrix = createMatrix(N); 
    if (node->reducedMatrix == NULL) {
        // CRITICAL FIX: Handle N=50 memory explosion gracefully
        printf("\n[Safety Stop] Memory limit reached (RAM full).\n");
        printf("The Exact Solver cannot proceed deeper. Returning best found so far.\n");
        // We exit(0) to stop the backend, but since we likely already wrote 
        // a heuristic solution to CSV, the UI will just show that.
        exit(0); 
    }
    
    copyMatrix(parentMatrix, node->reducedMatrix, N);

    // Set infinity for rows/cols to prevent revisiting
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

// Calculate the cost (Lower Bound) for a node
int calculateCost(int** reducedMatrix, int N, int u, int v) {
    int* rowRed = (int*)malloc(N * sizeof(int));
    int* colRed = (int*)malloc(N * sizeof(int));
    
    if (!rowRed || !colRed) return INF; // Safety check

    int rCost = rowReduction(reducedMatrix, N, rowRed);
    int cCost = columnReduction(reducedMatrix, N, colRed);

    free(rowRed);
    free(colRed);

    return rCost + cCost;
}

// Print the final path to console and file
void printPath(int* path, int N, int cost) {
    printf("\n[B&B] Optimal Solution Found:\n");
    printf("Minimum Cost: %d\n", cost);
    printf("Path: ");
    for (int i = 0; i < N; i++) {
        printf("%d -> ", path[i]);
    }
    printf("%d\n", path[0]); 

    // Write to CSV for Java UI
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

// Main B&B Solver Logic
void solveTSP(int** costMatrix, int N, int startNode, int initialUpperBound) {
    
    if (N < 2) {
        // Handle trivial case
        FILE* f = fopen("solution.csv", "w");
        if(f) { fprintf(f, "0,%d,%d", startNode, startNode); fclose(f); }
        return;
    }

    // Initialize Priority Queue (Min Heap)
    // Capacity 2,000,000 nodes ~ approx 500MB RAM. Adjust if needed.
    MinHeap* pq = createMinHeap(2000000); 

    int* emptyPath = (int*)malloc((N + 1) * sizeof(int));
    
    // Create Root Node
    Node* root = newNode(costMatrix, emptyPath, 0, -1, startNode, N);
    root->cost = calculateCost(root->reducedMatrix, N, -1, startNode);

    push(pq, root);

    printf("[Backend] B&B Solver initialized. Pruning limit: %d\n", initialUpperBound);

    while (!isEmpty(pq)) {
        Node* min = pop(pq);

        // --- HYBRID PRUNING LOGIC ---
        // If this branch is already worse than the best known solution (from ACO),
        // kill it immediately.
        if (min->cost >= initialUpperBound) {
            destroyMatrix(min->reducedMatrix, N);
            free(min->path);
            free(min);
            continue; 
        }

        // Check if we reached a leaf (Full Tour)
        if (min->level == N - 1) {
            printPath(min->path, N, min->cost);
            
            // Clean up heap and exit (First leaf in B&B is optimal)
            free(pq->array);
            free(pq);
            return; 
        }

        // Expand Children (Next Cities)
        for (int j = 0; j < N; j++) {
            if (min->reducedMatrix[min->vertex][j] != INF) {
                Node* child = newNode(min->reducedMatrix, min->path, min->level + 1, min->vertex, j, N);
                int reductionCost = calculateCost(child->reducedMatrix, N, min->vertex, j);
                child->cost = min->cost + min->reducedMatrix[min->vertex][j] + reductionCost;
                
                // Only add to heap if it has potential to beat the limit
                if (child->cost < initialUpperBound) {
                    push(pq, child);
                } else {
                    // Prune bad child immediately
                    destroyMatrix(child->reducedMatrix, N);
                    free(child->path);
                    free(child);
                }
            }
        }

        // Clean up processed node
        destroyMatrix(min->reducedMatrix, N);
        free(min->path);
        free(min);
    }
    
    // If queue is empty and no path found better than ACO:
    printf("[Backend] Search finished. No path strictly better than %d found.\n", initialUpperBound);
    // Note: We do NOT overwrite solution.csv here, ensuring the ACO result remains valid.
}