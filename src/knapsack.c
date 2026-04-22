#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../include/knapsack.h"

// Helper for sorting items by value-to-weight ratio (Density)
int compareItems(const void* a, const void* b) {
    Item* i1 = (Item*)a;
    Item* i2 = (Item*)b;
    if (i1->ratio < i2->ratio) return 1; 
    if (i1->ratio > i2->ratio) return -1;
    return 0;
}

// ==========================================
// FILE PARSER
// ==========================================
Item* parseKnapsackInput(const char* filename, int* N, int* W) {
    FILE* file = fopen(filename, "r");
    if (!file) {
        printf("[Error] Could not open knapsack input file: %s\n", filename);
        return NULL;
    }

    if (fscanf(file, "%d %d", N, W) != 2) {
        fclose(file);
        return NULL;
    }

    Item* items = (Item*)malloc((*N) * sizeof(Item));
    for (int i = 0; i < *N; i++) {
        items[i].id = i; 
        if (fscanf(file, "%d %d", &items[i].value, &items[i].weight) != 2) {
            break;
        }
        items[i].ratio = (double)items[i].value / items[i].weight;
    }

    fclose(file);
    return items;
}

// ==========================================
// MODE 2: GREEDY DENSITY HEURISTIC
// ==========================================
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
        for(int i = 0; i < N; i++) selectedItems[i] = 0;
        if(bestSingleIdx != -1) selectedItems[bestSingleIdx] = 1;
    } else {
        for(int i = 0; i < N; i++) selectedItems[i] = tempSelected[i];
    }
    
    free(tempSelected);
    return currentValue;
}

// ==========================================
// MODE 1 & 3: BRANCH & BOUND (EXACT/HYBRID)
// ==========================================
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
    
    for(int i = 0; i < N; i++) bestPath[i] = finalSelection[i];

    printf("[B&B] Starting search with floor: %d\n", maxProfit);

    while (front < rear) {
        if (rear >= MAX_NODES - 2) {
            printf("\n[Safety Stop] Queue Full. Returning best found profit.\n");
            break; 
        }

        u = queue[front++]; 

        if (u->bound <= (double)maxProfit) {
            free(u->taken);
            free(u);
            continue;
        }

        Node* v1 = (Node*)malloc(sizeof(Node));
        v1->level = u->level + 1;
        v1->weight = u->weight + items[v1->level].weight;
        v1->profit = u->profit + items[v1->level].value;
        v1->taken = (int*)malloc(N * sizeof(int));
        memcpy(v1->taken, u->taken, N * sizeof(int));
        v1->taken[items[v1->level].id] = 1; 

        if (v1->weight <= capacity && v1->profit > maxProfit) {
            maxProfit = v1->profit;
            for(int k=0; k<N; k++) bestPath[k] = v1->taken[k];
            printf("[B&B] New Best Profit Found: %d\n", maxProfit);
        }

        v1->bound = calculateBound(*v1, N, capacity, items);
        if (v1->bound > (double)maxProfit && v1->level < N - 1) {
            queue[rear++] = v1;
        } else {
            free(v1->taken);
            free(v1);
        }

        Node* v2 = (Node*)malloc(sizeof(Node));
        v2->level = u->level + 1;
        v2->weight = u->weight;
        v2->profit = u->profit;
        v2->taken = (int*)malloc(N * sizeof(int));
        memcpy(v2->taken, u->taken, N * sizeof(int)); 

        v2->bound = calculateBound(*v2, N, capacity, items);
        if (v2->bound > (double)maxProfit && v2->level < N - 1) {
            queue[rear++] = v2;
        } else {
            free(v2->taken);
            free(v2);
        }

        free(u->taken);
        free(u);
    }

    for(int i = 0; i < N; i++) finalSelection[i] = bestPath[i];

    free(queue);
    free(bestPath);
    return maxProfit;
}
// ==========================================
// UPGRADED MODE 3: CORE-PROBLEM HYBRID
// ==========================================
int solveKnapsack_AdvancedHybrid(Item* items, int N, int capacity, int* finalSelection) {
    // 1. Sort all items by density
    qsort(items, N, sizeof(Item), compareItems);

    int currentWeight = 0;
    int baseProfit = 0;
    int breakIndex = -1;

    // 2. Find the Break Item (where the bag overflows)
    for (int i = 0; i < N; i++) {
        if (currentWeight + items[i].weight > capacity) {
            breakIndex = i;
            break;
        }
        currentWeight += items[i].weight;
        baseProfit += items[i].value;
        finalSelection[items[i].id] = 1; 
    }

    // If it never broke, the Greedy solution is 100% perfect
    if (breakIndex == -1) return baseProfit;

    // 3. Isolate a Core of 40 items (20 before, 20 after the break)
    int CORE_RADIUS = 20; 
    int coreStart = (breakIndex - CORE_RADIUS < 0) ? 0 : breakIndex - CORE_RADIUS;
    int coreEnd = (breakIndex + CORE_RADIUS >= N) ? N - 1 : breakIndex + CORE_RADIUS;
    int coreSize = coreEnd - coreStart + 1;

    // 4. Calculate exactly what we permanently locked in BEFORE the core
    int remainingCapacity = capacity;
    int lockedProfit = 0;
    for (int i = 0; i < coreStart; i++) {
        remainingCapacity -= items[i].weight;
        lockedProfit += items[i].value;
    }
    
    // Clear the selection status for the core items so B&B starts fresh on them
    for (int i = coreStart; i <= coreEnd; i++) {
        finalSelection[items[i].id] = 0; 
    }

    // 5. Feed ONLY the tiny core slice to your exact Branch & Bound solver
    Item* coreItems = &items[coreStart]; 
    int* coreSelection = (int*)calloc(coreSize, sizeof(int));
    
    printf("[Core Engine] Massive dataset (N=%d) reduced down to Core Size=%d\n", N, coreSize);
    
    // Pass 0 as the initial lower bound since we are only solving a small isolated chunk
    int coreProfit = solveKnapsackBB(coreItems, coreSize, remainingCapacity, 0, coreSelection);

    // 6. Stitch the perfect core back into the global solution
    for (int i = 0; i < coreSize; i++) {
        if (coreSelection[i] == 1) {
            finalSelection[coreItems[i].id] = 1;
        }
    }
    
    free(coreSelection);
    return lockedProfit + coreProfit;
}
void writeKnapsackSolution(const char* filename, int profit, int* selection, int N) {
    FILE* f = fopen(filename, "w");
    if (f) {
        fprintf(f, "%d,", profit);
        for(int i = 0; i < N; i++) {
            fprintf(f, "%d%s", selection[i], (i == N - 1) ? "" : ",");
        }
        fclose(f);
        // Tell Java that the file is safely on the hard drive
        printf("[Backend] SUCCESS: Saved results to %s\n", filename);
    } else {
        // Scream if Windows blocks the file creation
        printf("[Backend] ERROR: Could not write to %s! Is it open in Excel?\n", filename);
    }
}