#include <stdio.h>
#include <stdlib.h>
#include "../include/knapsack.h"

// heuristic ta smart greedy er mto kaj korbe
int compareItems(const void* a, const void* b) {
    Item* i1 = (Item*)a;
    Item* i2 = (Item*)b;
    if (i1->ratio < i2->ratio) return 1; 
    return -1;
}

int solveGreedy(Item* items, int N, int capacity, int* selectedItems) {
    qsort(items, N, sizeof(Item), compareItems);

    int currentWeight = 0;
    int currentValue = 0;
    int* tempSelected = (int*)calloc(N, sizeof(int));

    for (int i = 0; i < N; i++) {
        if (currentWeight + items[i].weight <= capacity) {
            currentWeight += items[i].weight;
            currentValue += items[i].value;
            tempSelected[items[i].id] = 1;
        }
    }

    int maxSingleVal = 0;
    int bestSingleIdx = -1;
    for (int i = 0; i < N; i++) {
        if (items[i].weight <= capacity && items[i].value > maxSingleVal) {
            maxSingleVal = items[i].value;
            bestSingleIdx = items[i].id;
        }
    }

    if (maxSingleVal > currentValue) {
        currentValue = maxSingleVal;
        for(int i=0; i<N; i++) selectedItems[i] = 0;
        if(bestSingleIdx != -1) selectedItems[bestSingleIdx] = 1;
    } else {
        for(int i=0; i<N; i++) selectedItems[i] = tempSelected[i];
    }
    
    // Write greedy result to CSV
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

    int MAX_NODES = 2000000; 
    Node** queue = (Node**)malloc(MAX_NODES * sizeof(Node*));
    if (!queue) return initialLowerBound;

    int front = 0, rear = 0;

    Node* u = (Node*)malloc(sizeof(Node));
    u->level = -1;
    u->profit = 0;
    u->weight = 0;
    u->taken = (int*)calloc(N, sizeof(int));
    u->bound = calculateBound(*u, N, capacity, items);

    queue[rear++] = u;

    int maxProfit = initialLowerBound; 
    int* bestPath = (int*)malloc(N * sizeof(int));
    
    // ==========================================
    // CRITICAL HYBRID FIX: Inherit the Heuristic Path
    // ==========================================
    for(int i = 0; i < N; i++) {
        bestPath[i] = finalSelection[i]; 
    }
    // ==========================================

    printf("[B&B] Solver started. Pruning branches <= %d\n", maxProfit);

    while (front < rear) {
        if (rear >= MAX_NODES - 2) {
            printf("\n[Safety Stop] Memory limit reached (Queue Full).\n");
            break; 
        }

        u = queue[front++]; 

        if (u->bound <= maxProfit) {
            free(u->taken);
            free(u);
            continue;
        }

        Node* v1 = (Node*)malloc(sizeof(Node));
        v1->level = u->level + 1;
        v1->weight = u->weight + items[v1->level].weight;
        v1->profit = u->profit + items[v1->level].value;
        v1->taken = (int*)malloc(N * sizeof(int));
        for(int k=0; k<N; k++) v1->taken[k] = u->taken[k];
        v1->taken[items[v1->level].id] = 1; 

        if (v1->weight <= capacity && v1->profit > maxProfit) {
            maxProfit = v1->profit;
            for(int k=0; k<N; k++) bestPath[k] = v1->taken[k];
            printf("[B&B] New Best Profit: %d\n", maxProfit);
        }

        v1->bound = calculateBound(*v1, N, capacity, items);
        if (v1->bound > maxProfit) queue[rear++] = v1;
        else { free(v1->taken); free(v1); }

        Node* v2 = (Node*)malloc(sizeof(Node));
        v2->level = u->level + 1;
        v2->weight = u->weight;
        v2->profit = u->profit;
        v2->taken = (int*)malloc(N * sizeof(int));
        for(int k=0; k<N; k++) v2->taken[k] = u->taken[k]; 

        v2->bound = calculateBound(*v2, N, capacity, items);
        if (v2->bound > maxProfit) queue[rear++] = v2;
        else { free(v2->taken); free(v2); }

        free(u->taken);
        free(u);
    }

    // Overwrite CSV with best result (either Heuristic or new B&B path)
    FILE* f = fopen("solution_knapsack.csv", "w");
    if (f) {
        fprintf(f, "%d,", maxProfit);
        for(int i=0; i<N; i++) fprintf(f, "%d,", bestPath[i]);
        fclose(f);
    }

    free(queue);
    free(bestPath);
    return maxProfit;
}