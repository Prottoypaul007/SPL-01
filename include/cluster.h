#ifndef CLUSTER_H
#define CLUSTER_H

typedef struct {
    int medoid;      // The "center" city of this cluster
    int* cities;     // Array of global city indices assigned to this cluster
    int size;        // Current number of cities in the cluster
    int capacity;    // Memory capacity
    int* optimalPath; // Stores the local exact route for Phase 4 Stitching
} Cluster;

// Groups N cities into K clusters using K-Medoids
Cluster* clusterCities(int** matrix, int N, int K);

// Cleans up cluster memory
void freeClusters(Cluster* clusters, int K);

#endif