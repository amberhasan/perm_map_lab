# Equivalence Class Construction Method for U(n,3)

## Overview

This program constructs permutation arrays with minimum Ulam distance 3 using an **equivalence class approach**. It systematically explores the space of permutations by organizing them into equivalence classes based on where two specific symbols are placed.

## Algorithm Description

### Core Concept

The algorithm is based on the observation that permutations of length n can be constructed by:
1. Starting with a base permutation of length n-2
2. Inserting two new symbols (n-2 and n-1) in all possible positions
3. Each such insertion creates an "equivalence class" of related permutations

### How It Works

1. **Equivalence Class Generation**
   - For n=7, starts with permutations of {0,1,2,3,4} (length 5)
   - Inserts symbols 5 and 6 in all possible positions
   - Each base permutation generates 5×6 = 30 permutations in its class

2. **Iterative Construction**
   - Iterates through all (n-2)! base permutations
   - For each base:
     - Generates its equivalence class using `insert_two()`
     - Randomly shuffles the class (provides variation across iterations)
     - Greedily selects permutations from the class
     - Only adds a permutation if d(p,q) ≥ 3 for all q already in the code
     - Takes at most `MAX_PER_CLASS` permutations per class (currently 1)

3. **Randomized Search**
   - The shuffle provides randomization, so different iterations find different codes
   - Tracks the best (largest) code found across all iterations
   - Runs indefinitely until stopped (Ctrl+C)

### Key Features

- **Structured Exploration**: Unlike pure random search, this organizes the search space
- **Greedy Selection**: Only adds permutations that satisfy distance constraints
- **Automatic Saving**: Saves best solutions to file whenever a new best is found

## Ulam Distance Calculation

The program computes Ulam distance using the formula:

```
d(p,q) = n - LIS(p^{-1} ∘ q)
```

Where:
- `p^{-1}` is the inverse permutation of p
- `∘` denotes composition
- `LIS` is the Longest Increasing Subsequence

The LIS is computed using an efficient O(n log n) binary search algorithm.

## Usage

### Compilation

```bash
g++ -std=c++11 -O3 -o equiv_d3 equiv_d3.cpp
```

### Running

```bash
./equiv_d3 <n>
```

**Parameters:**
- `n` - The length of permutations (e.g., 7 for U(7,3))

**Example:**
```bash
./equiv_d3 7
```

### Stopping

Press `Ctrl+C` to stop the program. It will save the best result before exiting.

## Output Files

The program creates output files named: `U<n>_d3_equiv.txt`

**Example:** `U7_d3_equiv.txt`

**Format:**
```
# Best known U(7,3) >= 56
0 1 2 3 4 5 6
1 0 2 3 4 5 6
...
```

The first line is a comment showing the size. Each subsequent line is a permutation (space-separated integers).

## Configuration

You can modify these constants in the code:

```cpp
static const int D = 3;               // Minimum Ulam distance (currently fixed at 3)
static const int MAX_PER_CLASS = 1;   // Max permutations per equivalence class
```

- Increasing `MAX_PER_CLASS` to 2 or 3 may find larger codes but takes longer per iteration
- `D` is fixed at 3 for this implementation

## Performance Notes

- For n=7: Each iteration takes ~1-2 seconds and explores (7-2)! = 120 base permutations
- The algorithm is deterministic in structure but randomized in selection order
- Best results typically found after hundreds to thousands of iterations

## Algorithm Complexity

- **Time per iteration**: O((n-2)! × n² × code_size × n log n)
  - (n-2)! base permutations
  - n² permutations per equivalence class
  - For each candidate, check distance to all in code
  - Each distance check is O(n log n)

- **Space**: O(code_size × n)

## Results

Typical results for U(7,3):
- **Best known**: 56 permutations
- **Lower bound**: 59 (from theoretical bounds)
- **Gap**: The optimal value is still unknown

## Theory Background

This method exploits structure in the permutation space by recognizing that certain permutations are "similar" in how they're constructed (differing only in where two symbols are placed). This structured approach often outperforms pure random search for small values of n.

## Related Files

- `equiv_d3.cpp` - Source code with detailed comments
- `equiv_d3` - Compiled binary
- `U7_d3_equiv.txt` - Sample output for n=7
- `BEST_U7_3_equiv_d3.txt` - Best result found

## Author Notes

This implementation was developed for research on permutation arrays in the Ulam metric. The equivalence class approach provides a systematic way to explore the search space while maintaining enough randomization to find diverse solutions.

I created and used this program for U(7, 3). It got me a PA of size 56. Then I used the program improve_pa.cpp to get it to 64, beating the U(7, 3) result that was previously known to be 59. 