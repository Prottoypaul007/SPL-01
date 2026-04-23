#include <stdio.h>
#include <stdlib.h>

typedef struct Node {
    int level;
    int profit;
    int weight;
    double bound;
    int* taken; 
} Node;

int main() {
    int N = 40; // Simulating your Hybrid Core size
    long long nodeCount = 0;
    
    printf("Starting RAM Stress Test for N=%d...\n", N);
    
    // We use an array of pointers just like your MinHeap
    // We'll try to allocate up to 50 million nodes
    long long MAX_TEST = 50000000; 
    Node** memoryHog = (Node**)malloc(MAX_TEST * sizeof(Node*));
    
    while (nodeCount < MAX_TEST) {
        Node* u = (Node*)malloc(sizeof(Node));
        if (u == NULL) break; // OS denied memory for the struct
        
        u->taken = (int*)malloc(N * sizeof(int));
        if (u->taken == NULL) {
            free(u); // Clean up the orphaned struct
            break;   // OS denied memory for the array
        }
        
        memoryHog[nodeCount] = u;
        nodeCount++;
        
        if (nodeCount % 1000000 == 0) {
            printf("Successfully allocated %lld million nodes...\n", nodeCount / 1000000);
        }
    }
    
    printf("\n[HARDWARE LIMIT REACHED]\n");
    printf("Your PC successfully held %lld Nodes in RAM.\n", nodeCount);
    
    // Safe cleanup
    printf("Freeing memory to prevent system lockup...\n");
    for (long long i = 0; i < nodeCount; i++) {
        free(memoryHog[i]->taken);
        free(memoryHog[i]);
    }
    free(memoryHog);
    printf("Memory cleared. Test complete.\n");
    
    return 0;
}