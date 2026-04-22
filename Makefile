# ==========================================
# MASTER MAKEFILE: Optimization Algorithms Suite
# ==========================================
CC = gcc
# -Wno-unknown-pragmas silences OpenMP warnings if threading isn't active
CFLAGS = -Wall -O3 -Wno-unknown-pragmas -I./include

all: tsp_solver.exe knapsack_solver.exe

tsp_solver.exe: src/main.c src/parser.c src/matrix.c src/heap.c src/tsp.c src/aco.c src/cluster.c
	$(CC) $(CFLAGS) $^ -o $@
	@echo "[Make] Compiled TSP Engine successfully."

knapsack_solver.exe: src/main_knapsack.c src/knapsack.c
	$(CC) $(CFLAGS) $^ -o $@
	@echo "[Make] Compiled Knapsack Engine successfully."

clean:
	del /Q *.exe *.o solution*.csv *.class 2>nul
	@echo "[Make] Cleaned project directory."