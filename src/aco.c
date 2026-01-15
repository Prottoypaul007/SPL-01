#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <limits.h>
#include "../include/aco.h"
#define ANTS 20             
#define ITERATIONS 100      
#define ALPHA 1.0           // Pheromone
#define BETA 5.0            // Distance
#define EVAPORATION 0.5     
#define Q 100.0             
double power(double base, int exp) {
    double result = 1.0;
    for (int i = 0; i < exp; i++) {
        result *= base;
    }
    return result;
}
double prob(int current, int target, double** pheromones, int** dist) {
    if (dist[current][target] == 0) return 0.0;
    
    double tau = pheromones[current][target];         
    double eta = 1.0 / (double)dist[current][target];
    
    double tau_factor = tau; 
    double eta_factor = power(eta, (int)BETA); 

    return tau_factor * eta_factor;
}

void solveACO(int** matrix, int N, int startNode) {
    srand(time(NULL));

    double** pheromones = (double**)malloc(N * sizeof(double*));
    for (int i = 0; i < N; i++) {
        pheromones[i] = (double*)malloc(N * sizeof(double));
        for (int j = 0; j < N; j++) {
            pheromones[i][j] = 0.1;
        }
    }

    int* bestGlobalPath = (int*)malloc((N + 1) * sizeof(int));
    int bestGlobalCost = INT_MAX;

    printf("[ACO] Swarm initialized. Ants: %d, Iterations: %d\n", ANTS, ITERATIONS);

    for (int iter = 0; iter < ITERATIONS; iter++) {
        
        int** antPaths = (int**)malloc(ANTS * sizeof(int*));
        int* antCosts = (int*)malloc(ANTS * sizeof(int));

        for (int k = 0; k < ANTS; k++) {
            antPaths[k] = (int*)malloc((N + 1) * sizeof(int));
            int* visited = (int*)calloc(N, sizeof(int)); 
            
            int current = startNode; 
            antPaths[k][0] = current;
            visited[current] = 1;
            int cost = 0;

            for (int step = 0; step < N - 1; step++) {
                double sum = 0.0;
                    for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        sum += prob(current, j, pheromones, matrix);
                    }
                }

                double r = ((double)rand() / RAND_MAX) * sum;
                int nextCity = -1;
                double partialSum = 0.0;

                for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        partialSum += prob(current, j, pheromones, matrix);
                        if (partialSum >= r) {
                            nextCity = j;
                            break;
                        }
                    }
                }

                if (nextCity == -1) {
                    for (int x = 0; x < N; x++) if (!visited[x]) nextCity = x;
                }

                visited[nextCity] = 1;
                antPaths[k][step + 1] = nextCity;
                cost += matrix[current][nextCity];
                current = nextCity;
            }

            antPaths[k][N] = startNode;
            cost += matrix[current][startNode];
            antCosts[k] = cost;

            if (cost < bestGlobalCost) {
                bestGlobalCost = cost;
                for (int i = 0; i <= N; i++) bestGlobalPath[i] = antPaths[k][i];
            }

            free(visited);
        }

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                pheromones[i][j] = pheromones[i][j] * (1.0 - EVAPORATION); 
            }
        }

     
        for (int k = 0; k < ANTS; k++) {
            double deposit = Q / (double)antCosts[k];
            for (int i = 0; i < N; i++) {
                int u = antPaths[k][i];
                int v = antPaths[k][i+1];
                pheromones[u][v] = pheromones[u][v] + deposit;
                pheromones[v][u] = pheromones[v][u] + deposit;
            }
            free(antPaths[k]); 
        }
        free(antPaths);
        free(antCosts);
    }

    printf("\n--- ACO Result (Heuristic) ---\n");
    printf("Best Found Cost: %d\n", bestGlobalCost);
    printf("Path: ");
    for (int i = 0; i < N; i++) printf("%d -> ", bestGlobalPath[i]);
    printf("%d\n", bestGlobalPath[N]);
    FILE* f = fopen("solution.csv", "w");
    if (f != NULL) {
        fprintf(f, "%d,", bestGlobalCost);
        for (int i = 0; i < N; i++) {
            fprintf(f, "%d", bestGlobalPath[i]);
            if (i < N - 1) fprintf(f, ",");
        }
        fprintf(f, ",%d", bestGlobalPath[N]);
        fclose(f);
    }
    
    for(int i=0; i<N; i++) free(pheromones[i]);
    free(pheromones);
    free(bestGlobalPath);
}