#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include "../include/cluster.h"

void initClusters(Cluster* clusters, int K, int N) {
    for (int i = 0; i < K; i++) {
        clusters[i].capacity = N;
        clusters[i].cities = (int*)malloc(N * sizeof(int));
        clusters[i].size = 0;
        clusters[i].optimalPath = NULL; 
    }
}

Cluster* clusterCities(int** matrix, int N, int K) {
    Cluster* clusters = (Cluster*)malloc(K * sizeof(Cluster));
    initClusters(clusters, K, N);

    // Pick initial random medoids (centers)
    int* isMedoid = (int*)calloc(N, sizeof(int));
    for (int i = 0; i < K; i++) {
        int r;
        do { r = rand() % N; } while (isMedoid[r]);
        clusters[i].medoid = r;
        isMedoid[r] = 1;
    }

    int changed = 1;
    int iterations = 0;

    // K-Medoids Loop
    while (changed && iterations < 50) {
        changed = 0;
        iterations++;

        // Clear previous assignments
        for (int i = 0; i < K; i++) clusters[i].size = 0;

        // Assign each city to the nearest medoid
        for (int i = 0; i < N; i++) {
            int bestCluster = 0;
            int minDistance = INT_MAX;

            for (int k = 0; k < K; k++) {
                if (matrix[i][clusters[k].medoid] < minDistance) {
                    minDistance = matrix[i][clusters[k].medoid];
                    bestCluster = k;
                }
            }
            clusters[bestCluster].cities[clusters[bestCluster].size++] = i;
        }

        // Recalculate medoids
        for (int k = 0; k < K; k++) {
            int bestMedoid = clusters[k].medoid;
            long minTotalDist = LONG_MAX;

            for (int i = 0; i < clusters[k].size; i++) {
                int candidate = clusters[k].cities[i];
                long currentTotalDist = 0;

                for (int j = 0; j < clusters[k].size; j++) {
                    currentTotalDist += matrix[candidate][clusters[k].cities[j]];
                }

                if (currentTotalDist < minTotalDist) {
                    minTotalDist = currentTotalDist;
                    bestMedoid = candidate;
                }
            }

            if (bestMedoid != clusters[k].medoid) {
                clusters[k].medoid = bestMedoid;
                changed = 1;
            }
        }
    }

    free(isMedoid);
    printf("[Clustering] Grouped %d cities into %d zones in %d iterations.\n", N, K, iterations);
    return clusters;
}

void freeClusters(Cluster* clusters, int K) {
    for (int i = 0; i < K; i++) {
        free(clusters[i].cities);
        if (clusters[i].optimalPath != NULL) {
            free(clusters[i].optimalPath);
        }
    }
    free(clusters);
}