#include "../include/heap.h"
#include <stdlib.h>
#include <stdio.h>

MinHeap* createMinHeap(int capacity) {
    MinHeap* minHeap = (MinHeap*)malloc(sizeof(MinHeap));
    if (minHeap == NULL) {
        printf("[Error] Failed to allocate Minheap structure.\n");
        exit(1);
    }
    minHeap->size = 0;
    minHeap->capacity = capacity;
    minHeap->array = (Node**)malloc(capacity * sizeof(Node*));
    if (minHeap->array == NULL) {
        printf("[Error] Failed to allocate Heap Array for %d items.\n", capacity);
        exit(1);
    }
    return minHeap;
}

void swap(Node** a, Node** b) {
    Node* t = *a;
    *a = *b;
    *b = t;
}

void minHeapify(MinHeap* minHeap, int idx) {
    int smallest = idx;
    int left = 2 * idx + 1;
    int right = 2 * idx + 2;

    if (left < minHeap->size && minHeap->array[left]->cost < minHeap->array[smallest]->cost)
        smallest = left;

    if (right < minHeap->size && minHeap->array[right]->cost < minHeap->array[smallest]->cost)
        smallest = right;

    if (smallest != idx) {
        swap(&minHeap->array[smallest], &minHeap->array[idx]);
        minHeapify(minHeap, smallest);
    }
}

int isEmpty(MinHeap* minHeap) {
    return minHeap->size == 0;
}

Node* pop(MinHeap* minHeap) {
    if (isEmpty(minHeap)) return NULL;

    Node* root = minHeap->array[0];
    Node* lastNode = minHeap->array[minHeap->size - 1];
    minHeap->array[0] = lastNode;

    minHeap->size--;
    minHeapify(minHeap, 0);

    return root;
}

void push(MinHeap* minHeap, Node* node) {
    if (minHeap->size >= minHeap->capacity - 1) {
        printf("\n[Error] Heap Overflow. Reached  %d.\n", minHeap->capacity);
        printf("This input is too large.\n");
        exit(1); 
    }

    minHeap->size++;
    int i = minHeap->size - 1;

    while (i && node->cost < minHeap->array[(i - 1) / 2]->cost) {
        minHeap->array[i] = minHeap->array[(i - 1) / 2];
        i = (i - 1) / 2;
    }
    minHeap->array[i] = node;
}