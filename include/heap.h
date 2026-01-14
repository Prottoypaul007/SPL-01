#ifndef HEAP_H
#define HEAP_H

#include "tsp.h" 

typedef struct {
    Node** array;  
    int size;     
    int capacity; 
} MinHeap;

MinHeap* createMinHeap(int capacity);

void push(MinHeap* minHeap, Node* node);

Node* pop(MinHeap* minHeap);

int isEmpty(MinHeap* minHeap);
void destroyMinHeap(MinHeap* minHeap);

#endif