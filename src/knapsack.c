#include <stdio.h>
#include <stdlib.h>
#include "../include/knapsack.h"

// --- HEURISTIC: SMART GREEDY ---

int compareItems(const void* a, const void* b) {
    Item* i1 = (Item*)a;
    Item* i2 = (Item*)b;
    if (i1->ratio < i2->ratio) return 1; // Descending Order
    return -1;
}

int solveGreedy(Item* items, int N, int capacity, int* selectedItems) {
    // 1. Sort by Value/Weight ratio
    qsort(items, N, sizeof(Item), compareItems);

    int currentWeight = 0;
    int currentValue = 0;
    int* tempSelected = (int*)calloc(N, sizeof(int));

    // Strategy A: Standard Greedy Fill
    for (int i = 0; i < N; i++) {
        if (currentWeight + items[i].weight <= capacity) {
            currentWeight += items[i].weight;
            currentValue += items[i].value;
            tempSelected[items[i].id] = 1;
        }
    }

    // Strategy B: Single Best Item (Corner case check)
    int maxSingleVal = 0;
    int bestSingleIdx = -1;
    for (int i = 0; i < N; i++) {
        if (items[i].weight <= capacity && items[i].value > maxSingleVal) {
            maxSingleVal = items[i].value;
            bestSingleIdx = items[i].id;
        }
    }

    // Compare A vs B
    if (maxSingleVal > currentValue) {
        printf("\n[Heuristic] 'Single Best Item' strategy beat Standard Greedy.\n");
        currentValue = maxSingleVal;
        // Reset selection to just that one item
        for(int i=0; i<N; i++) selectedItems[i] = 0;
        if(bestSingleIdx != -1) selectedItems[bestSingleIdx] = 1;
    } else {
        // Keep Strategy A
        for(int i=0; i<N; i++) selectedItems[i] = tempSelected[i];
    }
    
    printf("\n[Heuristic] Smart Greedy Result: %d\n", currentValue);
    
    FILE* f = fopen("solution_knapsack.csv", "w");
    if (f) {
        fprintf(f, "%d,", currentValue);
        for(int i=0; i<N; i++) fprintf(f, "%d,", selectedItems[i]); 
        fclose(f);
    }

    free(tempSelected);
    return currentValue;
}

// --- EXACT: BRANCH & BOUND ---

typedef struct Node {
    int level;
    int profit;
    int weight;
    double bound;
    int* taken; 
} Node;

double calculateBound(Node u, int n, int capacity, Item* items) {
    if (u.weight >= capacity) return 0;

    double profitBound = u.profit;
    int j = u.level + 1;
    int totWeight = u.weight;

    while (j < n && totWeight + items[j].weight <= capacity) {
        totWeight += items[j].weight;
        profitBound += items[j].value;
        j++;
    }

    if (j < n) {
        profitBound += (capacity - totWeight) * items[j].ratio;
    }

    return profitBound;
}

int solveKnapsackBB(Item* items, int N, int capacity, int initialLowerBound, int* finalSelection) {
    qsort(items, N, sizeof(Item), compareItems);

    // Limit Queue Size to prevent RAM explosion
    int MAX_NODES = 2000000; 
    Node** queue = (Node**)malloc(MAX_NODES * sizeof(Node*));
    if (!queue) {
        printf("[Error] Not enough memory for B&B queue.\n");
        return initialLowerBound;
    }

    int front = 0, rear = 0;

    Node* u = (Node*)malloc(sizeof(Node));
    Node* v = (Node*)malloc(sizeof(Node));
    
    u->level = -1;
    u->profit = 0;
    u->weight = 0;
    u->taken = (int*)calloc(N, sizeof(int));
    u->bound = calculateBound(*u, N, capacity, items);

    queue[rear++] = u;

    int maxProfit = initialLowerBound; 
    int* bestPath = (int*)calloc(N, sizeof(int));

    printf("[B&B] Solver started. Pruning branches <= %d\n", maxProfit);

    while (front < rear) {
        // Safety Check: Queue Overflow
        if (rear >= MAX_NODES - 2) {
            printf("\n[Safety Stop] Memory limit reached (Queue Full).\n");
            printf("Returning best found result so far: %d\n", maxProfit);
            break; 
        }

        u = queue[front++]; 

        if (u->bound <= maxProfit) {
            free(u->taken);
            free(u);
            continue;
        }

        // Branch 1: Take Item
        v = (Node*)malloc(sizeof(Node));
        v->level = u->level + 1;
        v->weight = u->weight + items[v->level].weight;
        v->profit = u->profit + items[v->level].value;
        v->taken = (int*)malloc(N * sizeof(int));
        for(int k=0; k<N; k++) v->taken[k] = u->taken[k];
        v->taken[items[v->level].id] = 1; 

        if (v->weight <= capacity && v->profit > maxProfit) {
            maxProfit = v->profit;
            for(int k=0; k<N; k++) bestPath[k] = v->taken[k];
            printf("[B&B] New Best Profit: %d\n", maxProfit);
        }

        v->bound = calculateBound(*v, N, capacity, items);
        if (v->bound > maxProfit) queue[rear++] = v;
        else { free(v->taken); free(v); }

        // Branch 2: Don't Take Item
        v = (Node*)malloc(sizeof(Node));
        v->level = u->level + 1;
        v->weight = u->weight;
        v->profit = u->profit;
        v->taken = (int*)malloc(N * sizeof(int));
        for(int k=0; k<N; k++) v->taken[k] = u->taken[k]; // Copy history

        v->bound = calculateBound(*v, N, capacity, items);
        if (v->bound > maxProfit) queue[rear++] = v;
        else { free(v->taken); free(v); }

        free(u->taken);
        free(u);
    }

    // Write Final Result
    FILE* f = fopen("solution_knapsack.csv", "w");
    if (f) {
        fprintf(f, "%d,", maxProfit);
        for(int i=0; i<N; i++) fprintf(f, "%d,", bestPath[i]);
        fclose(f);
    }

    // Cleanup (Simplified for project scope)
    free(queue);
    free(bestPath);
    return maxProfit;
}