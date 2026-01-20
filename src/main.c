#include <stdio.h>
#include <stdlib.h>
#include "../include/parser.h"
#include "../include/matrix.h"
#include "../include/tsp.h"
#include "../include/aco.h"

int main(int argc, char* argv[]) {
    
    
    if (argc < 4) {
        printf("Usage: ./tsp_solver <file> <mode> <start_node>\n");
        return 1;
    }

    int N;
    int** matrix = parseInput(argv[1], &N);
    int mode = atoi(argv[2]);
    int startNode = atoi(argv[3]);

    if (!matrix) return 1;

    if (startNode < 0 || startNode >= N) {
        printf("[Error] Start node %d is invalid for N=%d. Defaulting to 0.\n", startNode, N);
        startNode = 0;
    }

    printf("[Backend] Loaded %d Cities. Start Node: %d\n", N, startNode);

    if (mode == 2) {
        printf("[Backend] Algorithm: Ant Colony Optimization (Heuristic)\n");
        solveACO(matrix, N, startNode);
    } 
    else {
        if (N > 15) {
            printf("[Warning] N=%d is too large for Exact Solver. Switching to ACO.\n", N);
            solveACO(matrix, N, startNode);
        } else {
            printf("[Backend] Algorithm: Branch & Bound (Exact)\n");
            solveTSP(matrix, N, startNode);
        }
    }

    destroyMatrix(matrix, N);
    return 0;
}