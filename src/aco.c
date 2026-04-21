#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <limits.h>
#include <omp.h>
#include "../include/aco.h"

// ====== PARAMETERS ======
#define ANTS 20
#define ITERATIONS 100
#define BETA 5.0
#define EVAPORATION 0.5
#define Q 100.0

// ====== HELPER FUNCTION: power(base, exp) ======
double power(double base, int exp) {
    double result = 1.0;
    for (int i = 0; i < exp; i++) {
        result *= base;
    }
    return result;
}

// ====== PROBABILITY FUNCTION ======
// Calculates desirability of going from current → target
double prob(int current, int target, double** pheromones, int** dist) {
    if (dist[current][target] == 0) return 0.0;

    double tau = pheromones[current][target];     // pheromone
    double eta = 1.0 / (double)dist[current][target]; // heuristic (1/distance)

    return tau * power(eta, (int)BETA);
}

// ====== MAIN ACO FUNCTION ======
int solveACO(int** matrix, int N, int startNode, int silent) {

    srand(time(NULL));

    // ====== INITIALIZE PHEROMONES ======
    double** pheromones = (double**)malloc(N * sizeof(double*));
    for (int i = 0; i < N; i++) {
        pheromones[i] = (double*)malloc(N * sizeof(double));
        for (int j = 0; j < N; j++) {
            pheromones[i][j] = 0.1;  // initial pheromone
        }
    }

    // ====== GLOBAL BEST ======
    int* bestGlobalPath = (int*)malloc((N + 1) * sizeof(int));
    int bestGlobalCost = INT_MAX;

    if (!silent) {
        printf("[ACO] Ants: %d | Iterations: %d | Multithreaded\n", ANTS, ITERATIONS);
    }

    // ====== MAIN LOOP ======
    for (int iter = 0; iter < ITERATIONS; iter++) {

        int** antPaths = (int**)malloc(ANTS * sizeof(int*));
        int* antCosts = (int*)malloc(ANTS * sizeof(int));

        // ====== PARALLEL ANT SIMULATION ======
        #pragma omp parallel for
        for (int k = 0; k < ANTS; k++) {

            antPaths[k] = (int*)malloc((N + 1) * sizeof(int));
            int* visited = (int*)calloc(N, sizeof(int));

            int current = startNode;
            antPaths[k][0] = current;
            visited[current] = 1;

            int cost = 0;

            // ====== BUILD PATH ======
            for (int step = 0; step < N - 1; step++) {

                double totalProb = 0.0;

                // Calculate total probability
                for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        totalProb += prob(current, j, pheromones, matrix);
                    }
                }

                // Random selection
                double r = ((double)rand() / RAND_MAX) * totalProb;

                int nextCity = -1;
                double cumulative = 0.0;

                // Roulette wheel selection
                for (int j = 0; j < N; j++) {
                    if (!visited[j]) {
                        cumulative += prob(current, j, pheromones, matrix);
                        if (cumulative >= r) {
                            nextCity = j;
                            break;
                        }
                    }
                }

                // Fallback (if something goes wrong)
                if (nextCity == -1) {
                    for (int x = 0; x < N; x++) {
                        if (!visited[x]) {
                            nextCity = x;
                            break;
                        }
                    }
                }

                // Move to next city
                visited[nextCity] = 1;
                antPaths[k][step + 1] = nextCity;
                cost += matrix[current][nextCity];
                current = nextCity;
            }

            // Return to start node
            antPaths[k][N] = startNode;
            cost += matrix[current][startNode];

            antCosts[k] = cost;

            // ====== UPDATE GLOBAL BEST (THREAD SAFE) ======
            #pragma omp critical
            {
                if (cost < bestGlobalCost) {
                    bestGlobalCost = cost;
                    for (int i = 0; i <= N; i++) {
                        bestGlobalPath[i] = antPaths[k][i];
                    }
                }
            }

            free(visited);
        }

        // ====== PHEROMONE EVAPORATION ======
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                pheromones[i][j] *= (1.0 - EVAPORATION);
            }
        }

        // ====== PHEROMONE DEPOSIT ======
        for (int k = 0; k < ANTS; k++) {

            double deposit = Q / (double)antCosts[k];

            for (int i = 0; i < N; i++) {
                int u = antPaths[k][i];
                int v = antPaths[k][i + 1];

                pheromones[u][v] += deposit;
                pheromones[v][u] += deposit;
            }

            free(antPaths[k]);
        }

        free(antPaths);
        free(antCosts);
    }

    // ====== OUTPUT ======
    if (!silent) {
        printf("\nBest Cost: %d\n", bestGlobalCost);
    }

    // Save result to file
    if (!silent) {
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
    }

    // ====== FREE MEMORY ======
    for (int i = 0; i < N; i++) {
        free(pheromones[i]);
    }
    free(pheromones);
    free(bestGlobalPath);

    return bestGlobalCost;
}