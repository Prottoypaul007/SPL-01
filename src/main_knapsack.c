#include <stdio.h>
#include <stdlib.h>
#include "../include/knapsack.h"

int main(int argc, char* argv[]) {
    // 1. Validate Command Line Arguments
    if (argc < 4) {
        printf("Usage: ./knapsack_solver <file> <mode> <capacity>\n");
        return 1;
    }

    char* filename = argv[1];
    int mode = atoi(argv[2]);        // 1 = Exact, 2 = Heuristic, 3 = Hybrid
    int capacity = atoi(argv[3]);

    // 2. Read Input Data from File
    FILE* f = fopen(filename, "r");
    if (!f) {
        printf("[Error] Cannot open input file: %s\n", filename);
        return 1;
    }

    int N;
    fscanf(f, "%d", &N);
    
    Item* items = (Item*)malloc(N * sizeof(Item));
    for (int i = 0; i < N; i++) {
        items[i].id = i; // Keep track of original index before sorting
        fscanf(f, "%d %d", &items[i].value, &items[i].weight);
        
        // Calculate efficiency ratio for the Heuristic
        if (items[i].weight > 0) {
            items[i].ratio = (double)items[i].value / items[i].weight;
        } else {
            items[i].ratio = 0;
        }
    }
    fclose(f);

    int* result = (int*)calloc(N, sizeof(int)); // Array to store 1 (selected) or 0 (not selected)

    printf("[Backend] Loaded %d items. Bag Capacity: %d\n", N, capacity);

    // ==========================================
    // 3. AUTO-SWITCH LOGIC (Crash Prevention)
    // ==========================================
    if (mode == 1) { // User selected "Exact"
        if (N > 40) {
            printf("[Auto-Switch] N=%d is too massive for pure Exact B&B.\n", N);
            printf("Switching to Hybrid Mode to prevent system crash.\n");
            mode = 3; 
        }
    }

    // ==========================================
    // 4. ALGORITHM EXECUTION
    // ==========================================
    if (mode == 2) {
        // --- MODE 2: HEURISTIC ONLY ---
        printf("[Backend] Mode: Heuristic (Smart Greedy)\n");
        solveGreedy(items, N, capacity, result);
    }
    else if (mode == 3) {
        // --- MODE 3: HYBRID (Safe) ---
        printf("[Backend] Mode: Hybrid (Smart Greedy + Conditional B&B)\n");
        
        // Phase 1: Smart Greedy Warm Start
        printf("--- Phase 1: Running Heuristic to find Lower Bound ---\n");
        int heuristicProfit = solveGreedy(items, N, capacity, result);
        printf("[Hybrid] Guaranteed Minimum Profit found: %d\n", heuristicProfit);

        // Phase 2: Safety Check before Exact Solver
        if (N > 45) {
            // If N is still too massive, skip the Exact phase entirely to save RAM
            printf("\n[Safety Stop] N=%d is too massive for the Exact Phase.\n", N);
            printf("Skipping Branch & Bound. Returning Optimized Heuristic Result to UI.\n");
            // The result is already written to the CSV by solveGreedy, so we just exit safely.
        } else {
            // Safe to run Exact B&B
            printf("--- Phase 2: Running Exact Solver with Pruning Bound %d ---\n", heuristicProfit);
            // Pass the heuristicProfit as the starting 'Lower Bound' for B&B to use for pruning
            solveKnapsackBB(items, N, capacity, heuristicProfit, result);
        }
    }
    else {
        // --- MODE 1: EXACT ONLY ---
        printf("[Backend] Mode: Exact (Branch & Bound)\n");
        // Pass -1 as the lower bound because we have no heuristic to guide us
        solveKnapsackBB(items, N, capacity, -1, result);
    }

    // 5. Memory Cleanup
    free(items);
    free(result);
    return 0;
}