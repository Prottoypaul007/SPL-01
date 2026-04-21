# Compiler settings
CC = gcc
CFLAGS = -Wall -O3 -I./include
# Target executables
TSP_EXEC = tsp_solver.exe
KNAP_EXEC = knapsack_solver.exe

# Source files
TSP_SRC = src/main.c src/parser.c src/matrix.c src/heap.c src/tsp.c src/aco.c
KNAP_SRC = src/main_knapsack.c src/knapsack.c

# Default target builds both
all: $(TSP_EXEC) $(KNAP_EXEC)

# Build TSP
$(TSP_EXEC): $(TSP_SRC)
	$(CC) $(CFLAGS) $(TSP_SRC) -o $(TSP_EXEC)
	@echo "Compiled $(TSP_EXEC) successfully."

# Build Knapsack
$(KNAP_EXEC): $(KNAP_SRC)
	$(CC) $(CFLAGS) $(KNAP_SRC) -o $(KNAP_EXEC)
	@echo "Compiled $(KNAP_EXEC) successfully."

# Clean up compiled files
clean:
	del $(TSP_EXEC) $(KNAP_EXEC) input.txt knapsack_input.txt solution.csv solution_knapsack.csv