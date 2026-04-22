#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include "../include/parser.h"
#include "../include/matrix.h"
#include "../include/tsp.h"
#include "../include/aco.h"
#include "../include/cluster.h"

int* readPathFromCSV(int expectedSize, int* outCost) {
    FILE* f = fopen("solution.csv", "r");
    if (!f) return NULL;
    
    int* path = (int*)malloc(expectedSize * sizeof(int));
    if (fscanf(f, "%d,", outCost) != 1) {
        fclose(f);
        free(path);
        return NULL;
    }
    
    for(int i = 0; i < expectedSize; i++) {
        fscanf(f, "%d,", &path[i]);
    }
    fclose(f);
    return path;
}

int main(int argc, char* argv[]) {
    if (argc < 4) {
        printf("Usage: ./tsp_solver <file> <mode> <start_node>\n");
        return 1;
    }

    int N;
    int** matrix = parseInput(argv[1], &N);
    int mode = atoi(argv[2]); 
    int startNode = atoi(argv[3]);

    if (!matrix) return 1;
    if (startNode < 0 || startNode >= N) startNode = 0;

    printf("[Backend] Loaded %d Cities. Start Node: %d\n", N, startNode);
    if (mode == 1 && N > 22) {
        printf("[Auto-Switch] N=%d is massive. Switching to Clustered Hybrid (Mode 3).\n", N);
        mode = 3; 
    }

    if (mode == 2) {
        printf("[Backend] Mode: Ant Colony Optimization (Heuristic)\n");
        solveACO(matrix, N, startNode, 0); 
    } 
    else if (mode == 3) {
        printf("[Backend] Mode: Massive Scale Hybrid (K-Medoids + B&B + ACO)\n");

        if (N <= 20) {
            solveTSP(matrix, N, startNode, INT_MAX);
        } else {
            int K = (N / 12) + 1; 
            Cluster* zones = clusterCities(matrix, N, K);
            //Micro-Routing
            for (int i = 0; i < K; i++) {
                int size = zones[i].size;
                
                // Safety 1: STRICT guards for empty/tiny clusters
                if (size == 0) {
                    zones[i].optimalPath = NULL;
                    continue;
                }
                if (size == 1) {
                    zones[i].optimalPath = (int*)malloc(sizeof(int));
                    zones[i].optimalPath[0] = zones[i].cities[0];
                    continue;
                }

                printf("\n--- Solving Local Zone %d (Size: %d, Center: %d) ---\n", i, size, zones[i].medoid);
                
                int** microMatrix = createMatrix(size);
                for (int u = 0; u < size; u++) {
                    for (int v = 0; v < size; v++) {
                        microMatrix[u][v] = matrix[zones[i].cities[u]][zones[i].cities[v]];
                    }
                }
                                remove("solution.csv");

                if (size > 12) {
                    printf("[Fallback] Cluster too large (%d)! Using ACO for this zone.\n", size);
                    solveACO(microMatrix, size, 0, 0); 
                } else {
                    //ram thik rakhar jnne ACO cholbe
                    int tightBound = INT_MAX;
                    for (int a = 0; a < 30; a++) {
                        int cost = solveACO(microMatrix, size, 0, 1); // 1 = silent mode
                        if (cost < tightBound) tightBound = cost;
                    }
                    printf("      [Pruning] Warm-start bound locked at: %d\n", tightBound);
                    solveTSP(microMatrix, size, 0, tightBound); 
                }
                int dummyCost;
                int* localPath = readPathFromCSV(size, &dummyCost);
                if (localPath != NULL) {
                    zones[i].optimalPath = (int*)malloc(size * sizeof(int));
                    for(int p = 0; p < size; p++) {
                        zones[i].optimalPath[p] = zones[i].cities[localPath[p]];
                    }
                    free(localPath);
                } else {
                    zones[i].optimalPath = NULL;
                }
                
                destroyMatrix(microMatrix, size);
            }
            printf("\n--- Connecting %d zones via Ant Colony ---\n", K);
            int** macroMatrix = createMatrix(K);
            for (int u = 0; u < K; u++) {
                for (int v = 0; v < K; v++) {
                    macroMatrix[u][v] = matrix[zones[u].medoid][zones[v].medoid];
                }
            }
            
            remove("solution.csv");
            solveACO(macroMatrix, K, 0, 0); 
            int dummyMacroCost;
            int* macroPath = readPathFromCSV(K, &dummyMacroCost);
            destroyMatrix(macroMatrix, K);            
            if (macroPath != NULL) {
                printf("[Stitching] Assembling cities with jump optimization...\n");
                int* finalRoute = (int*)malloc(N * sizeof(int));
                int idx = 0;
                
                for (int m = 0; m < K; m++) {
                    int clusterId = macroPath[m];
                    int size = zones[clusterId].size;                    
                    if (size == 0 || zones[clusterId].optimalPath == NULL) continue;                    
                    if (idx == 0) { 
                        for (int p = 0; p < size; p++) {
                            finalRoute[idx++] = zones[clusterId].optimalPath[p];
                        }
                    } else {
                        int lastCity = finalRoute[idx - 1];
                        int bestStartIndex = 0;
                        int minJumpDist = INT_MAX;
                        
                        for (int p = 0; p < size; p++) {
                            int candidateCity = zones[clusterId].optimalPath[p];
                            if (matrix[lastCity][candidateCity] < minJumpDist) {
                                minJumpDist = matrix[lastCity][candidateCity];
                                bestStartIndex = p;
                            }
                        }
                        for (int p = 0; p < size; p++) {
                            int rotatedIndex = (bestStartIndex + p) % size;
                            finalRoute[idx++] = zones[clusterId].optimalPath[rotatedIndex];
                        }
                    }
                }
                if (idx > 0) {
                    int finalCost = 0;
                    for (int i = 0; i < idx - 1; i++) {
                        finalCost += matrix[finalRoute[i]][finalRoute[i+1]];
                    }
                    finalCost += matrix[finalRoute[idx-1]][finalRoute[0]]; 
                    
                    FILE* finalOut = fopen("solution.csv", "w");
                    if (finalOut) {
                        fprintf(finalOut, "%d,", finalCost);
                        for (int i = 0; i < idx; i++) {
                            fprintf(finalOut, "%d,", finalRoute[i]);
                        }
                        fprintf(finalOut, "%d\n", finalRoute[0]); 
                        fclose(finalOut);
                    }
                }
                free(finalRoute);
                free(macroPath);
            }
            freeClusters(zones, K);
        }
    } else {
        printf("[Backend] Mode: Exact (Branch & Bound)\n");
        solveTSP(matrix, N, startNode, INT_MAX);
    }

    destroyMatrix(matrix, N);
    return 0;
}