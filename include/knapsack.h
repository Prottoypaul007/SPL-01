#ifndef KNAPSACK_H
#define KNAPSACK_H

// 1. Define the struct first
typedef struct {
    int id;
    int value;
    int weight;
    double ratio;
} Item;

// 2. Align the prototypes to use Item*
Item* parseKnapsackInput(const char* filename, int* N, int* W);

int solveGreedy(Item* items, int N, int capacity, int* selectedItems);

int solveKnapsackBB(Item* items, int N, int capacity, int initialLowerBound, int* finalSelection);

// ADD THIS NEW LINE:
int solveKnapsack_AdvancedHybrid(Item* items, int N, int capacity, int* finalSelection);

void writeKnapsackSolution(const char* filename, int profit, int* selection, int N);

#endif