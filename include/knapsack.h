#ifndef KNAPSACK_H
#define KNAPSACK_H

typedef struct {
    int id;
    int value;
    int weight;
    double ratio;
} Item;

// Heuristic: Returns the profit found
int solveGreedy(Item* items, int N, int capacity, int* selectedItems);

// Exact/Hybrid: Returns optimal profit, accepts initialLowerBound (from Heuristic)
int solveKnapsackBB(Item* items, int N, int capacity, int initialLowerBound, int* selectedItems);

#endif