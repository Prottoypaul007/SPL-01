#include <stdio.h>
#include <stdlib.h>
#include "../include/knapsack.h"

int main(int argc, char* argv[]) {
    if (argc < 4) {
        printf("Usage: ./knapsack_solver <file> <mode> <capacity>\n");
        return 1;
    }

    char* filename = argv[1];
    int mode = atoi(argv[2]);
    int capacity = atoi(argv[3]);

    FILE* f = fopen(filename, "r");
    if (!f) return 1;

    int N;
    fscanf(f, "%d", &N);
    Item* items = (Item*)malloc(N * sizeof(Item));
    for (int i = 0; i < N; i++) {
        items[i].id = i;
        fscanf(f, "%d %d", &items[i].value, &items[i].weight);
        items[i].ratio = (double)items[i].value / items[i].weight;
    }
    fclose(f);

    int* result = (int*)calloc(N, sizeof(int));

    printf("[Backend] Loaded %d items. Capacity: %d\n", N, capacity);

    // --- AUTO-SWITCH LOGIC ---
    if (mode == 1) { // User wanted Exact
        if (N > 40) {
            printf("[Auto-Switch] N=%d is too large for Exact B&B. Switching to Hybrid.\n", N);
            mode = 3; 
        }
    }

    if (mode == 2) {
        // --- HEURISTIC ONLY ---
        printf("[Backend] Mode: Heuristic (Smart Greedy)\n");
        solveGreedy(items, N, capacity, result);
    }
    else if (mode == 3) {
        // --- HYBRID (Safe) ---
        printf("[Backend] Mode: Hybrid (Greedy + Conditional B&B)\n");
        
        // Step 1: Greedy Warm Start
        int heuristicProfit = solveGreedy(items, N, capacity, result);
        printf("[Hybrid] Greedy found Lower Bound: %d\n", heuristicProfit);

        // Step 2: Safety Check
        if (N > 45) {
            printf("\n[Safety Stop] N=%d is too massive for Exact Phase.\n", N);
            printf("Returning Smart Greedy Result to prevent crash.\n");
            // We do not run solveKnapsackBB. Result is already written by solveGreedy.
        } else {
            printf("--- Phase 2: Running Exact Solver with Bound %d ---\n", heuristicProfit);
            solveKnapsackBB(items, N, capacity, heuristicProfit, result);
        }
    }
    else {
        // --- EXACT ONLY ---
        printf("[Backend] Mode: Exact (Branch & Bound)\n");
        solveKnapsackBB(items, N, capacity, -1, result);
    }

    free(items);
    free(result);
    return 0;
}