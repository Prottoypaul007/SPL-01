#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../include/knapsack.h"

//value-to-weight ratio density onujayi sorting 
int compareItems(const void* a, const void* b) {
    Item* i1 = (Item*)a;
    Item* i2 = (Item*)b;
    if (i1->ratio < i2->ratio) return 1; 
    if (i1->ratio > i2->ratio) return -1;
    return 0;
}

// file parser
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

//greedy density heuristic
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

// b&B
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
    u->level = -1;//creat a dummy rootnode
    u->profit = 0;
    u->weight = 0;
    u->taken = (int*)calloc(N, sizeof(int));
    u->bound = calculateBound(*u, N, capacity, items);

    queue[rear++] = u;

    int maxProfit = initialLowerBound; 
    int* bestPath = (int*)calloc(N, sizeof(int)); // Local index tracking
    
    printf("[B&B] Starting search with floor: %d\n", maxProfit);

    while (front < rear) { //breadth first search loop and pruning
        if (rear >= MAX_NODES - 2) {
            printf("\n-Safety Stop- Queue Full. Returning best found profit.\n");
            break; 
        }

        u = queue[front++]; 

        if (u->bound <= (double)maxProfit) {
            free(u->taken);
            free(u);
            continue;
        }

        // node-We take the item
        Node* v1 = (Node*)malloc(sizeof(Node));
        v1->level = u->level + 1;
        v1->weight = u->weight + items[v1->level].weight;
        v1->profit = u->profit + items[v1->level].value;
        v1->taken = (int*)malloc(N * sizeof(int));
        memcpy(v1->taken, u->taken, N * sizeof(int));
        
        //Track by local tree level, NOT global ID
        v1->taken[v1->level] = 1; 

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

        // Node v2: We LEAVE the item
        Node* v2 = (Node*)malloc(sizeof(Node));
        v2->level = u->level + 1;
        v2->weight = u->weight;
        v2->profit = u->profit;
        v2->taken = (int*)malloc(N * sizeof(int));
        memcpy(v2->taken, u->taken, N * sizeof(int)); 
        
        v2->taken[v2->level] = 0; // Local index

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

    //Translate your local tree results back to the user's original data.
  for(int i = 0; i < N; i++) {
        finalSelection[items[i].id] = bestPath[i];
    }

    free(queue);
    free(bestPath);
    return maxProfit;
}
//hybrid
int solveKnapsack_AdvancedHybrid(Item* items, int N, int capacity, int* finalSelection) {
    qsort(items, N, sizeof(Item), compareItems);

    int currentWeight = 0;
    int baseProfit = 0;
    int breakIndex = -1;

    for (int i = 0; i < N; i++) {
        if (currentWeight + items[i].weight > capacity) {
            breakIndex = i;
            break;
        }
        currentWeight += items[i].weight;
        baseProfit += items[i].value;
        finalSelection[items[i].id] = 1; 
    }

    if (breakIndex == -1) return baseProfit;

    int CORE_RADIUS = 20; 
    int coreStart = (breakIndex - CORE_RADIUS < 0) ? 0 : breakIndex - CORE_RADIUS;
    int coreEnd = (breakIndex + CORE_RADIUS >= N) ? N - 1 : breakIndex + CORE_RADIUS;
    int coreSize = coreEnd - coreStart + 1;
/*You set a CORE_RADIUS of 20. This means you look 20 items to the left of the break index, and 20 items to the right, creating a window of 40 items.
The Ternary Operators (? :): This is excellent edge-case protection. If the break index is very close to the start or end of the array, breakIndex - 20 might result in a negative number, causing a segmentation fault. These checks ensure the core stays within the array bounds.*/

    int remainingCapacity = capacity;
    int lockedProfit = 0;
    for (int i = 0; i < coreStart; i++) {
        remainingCapacity -= items[i].weight;
        lockedProfit += items[i].value;
    }
    
    for (int i = coreStart; i <= coreEnd; i++) {
        finalSelection[items[i].id] = 0; 
    }

    Item* coreItems = &items[coreStart]; 
    printf("[Core Engine] Massive dataset (N=%d) reduced down to Core Size=%d\n", N, coreSize);
    
    // Pass the global array directly in. B&B will safely map back to the global IDs.
    int coreProfit = solveKnapsackBB(coreItems, coreSize, remainingCapacity, 0, finalSelection);

    return lockedProfit + coreProfit;
}
// CSV WRITER 
void writeKnapsackSolution(const char* filename, int profit, int* selection, int N) {
    FILE* f = fopen(filename, "w");
    if (f) {
        fprintf(f, "%d,", profit);
        for(int i = 0; i < N; i++) {
            fprintf(f, "%d%s", selection[i], (i == N - 1) ? "" : ",");
        }
        fclose(f);
        printf("[Backend] SUCCESS: Saved results to %s\n", filename);
    } else {
        printf("[Backend] ERROR: Could not write to %s! Is it open in Excel?\n", filename);
    }
}