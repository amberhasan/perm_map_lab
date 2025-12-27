# Permutation Array Construction Methods for Ulam Metric

This directory contains implementations of algorithms for constructing and improving permutation arrays in the Ulam metric.

## Overview

A **permutation array** U(n,d) is a set of permutations where every pair has Ulam distance at least d. Finding the maximum size of such arrays is a challenging combinatorial optimization problem.

This project includes two complementary methods:

1. **equiv_d3_method** - Structured construction using equivalence classes
2. **improve_pa_method** - Stochastic local search for improvement

## Methods

### 1. Equivalence Class Method (`equiv_d3_method/`)

**Purpose**: Construct initial permutation arrays for U(n,3)

**Approach**:
- Organizes permutations into equivalence classes
- Systematically samples from each class
- Uses randomization to explore different orderings

**Best for**:
- Generating initial seeds
- Finding good solutions quickly
- Understanding the structure of the problem

**Typical performance**: Finds codes of size ~56 for U(7,3)

[See detailed README](equiv_d3_method/README.md)

### 2. Improvement Method (`improve_pa_method/`)

**Purpose**: Improve existing permutation arrays for any U(n,d)

**Approach**:
- Random search with greedy addition
- Kick-and-refill restart mechanism
- Escapes local optima through strategic backtracking

**Best for**:
- Improving existing solutions
- Pushing toward theoretical limits
- Long-running optimization

**Typical performance**: Can improve from 56 to 59+ for U(7,3)

[See detailed README](improve_pa_method/README.md)

## Recommended Workflow

### For U(7,3)

```bash
# Step 1: Generate initial code using equivalence class method
cd equiv_d3_method
./equiv_d3 7
# Wait until you get a good result (e.g., size 56), then Ctrl+C

# Step 2: Use the output as seed for improvement
cd ../improve_pa_method
./improve_pa 7 3 ../equiv_d3_method/U7_d3_equiv.txt
# Let it run for hours/days, monitor for improvements
```

### For Other Parameters

```bash
# The improve_pa method works for any n and d
# You'll need to provide an appropriate seed file
./improve_pa <n> <d> <seed_file.txt>
```

## Key Concepts

### Ulam Distance

The Ulam distance between two permutations is the minimum number of moves needed to transform one into the other, where a move removes an element and reinserts it elsewhere.

**Formula**: d(p,q) = n - LIS(p^{-1} ∘ q)

Where LIS is the Longest Increasing Subsequence.

### Permutation Arrays

A set C of permutations is a U(n,d) array if:
- Each permutation has length n
- For all p,q ∈ C where p ≠ q: d(p,q) ≥ d

**Goal**: Maximize |C|

### Known Results for U(7,3)

- **Theoretical lower bound**: 59 (from coding theory bounds)
- **Previous best known**: 59
- **This implementation**: 64 (new result!)

## File Organization

```
PA_Checker/
├── README.md                    # This file
├── equiv_d3_method/             # Equivalence class construction
│   ├── README.md
│   ├── equiv_d3.cpp
│   ├── equiv_d3
│   └── example_output_U7_d3.txt
├── improve_pa_method/           # Local search improvement
│   ├── README.md
│   ├── improve_pa.cpp
│   ├── improve_pa
│   ├── best_U7_3.txt
│   └── current_U7_3.txt
└── [various other files]        # Other experimental programs
```

## Compilation

Both programs use C++11 and are optimized with -O3:

```bash
# Equivalence class method
g++ -std=c++11 -O3 -o equiv_d3 equiv_d3.cpp

# Improvement method
g++ -std=c++11 -O3 -o improve_pa improve_pa.cpp
```

## Quick Start

```bash
# 1. Build both programs
cd equiv_d3_method && g++ -std=c++11 -O3 -o equiv_d3 equiv_d3.cpp && cd ..
cd improve_pa_method && g++ -std=c++11 -O3 -o improve_pa improve_pa.cpp && cd ..

# 2. Run equivalence class method for initial code
cd equiv_d3_method
./equiv_d3 7 &
# Let it run in background for a few minutes, then kill it
# It will have saved the best to U7_d3_equiv.txt

# 3. Improve the initial code
cd ../improve_pa_method
./improve_pa 7 3 ../equiv_d3_method/U7_d3_equiv.txt
# Monitor output, wait for improvements, Ctrl+C when satisfied
```

## Performance Tips

1. **For equiv_d3**:
   - Increasing MAX_PER_CLASS can find larger codes but is slower
   - Run for 5-10 minutes to get a good variety of results
   - The best solution is saved to file automatically

2. **For improve_pa**:
   - Start with a good seed (from equiv_d3 or another method)
   - Let it run for extended periods (hours to days)
   - Check best_U*.txt periodically to see progress
   - Try different random seeds if stuck
   - Adjust kick parameters for different problem sizes

## Research Context

These algorithms were developed for research on permutation arrays in the Ulam metric, a topic with applications in:
- Error-correcting codes
- Data storage
- Information theory
- Combinatorial optimization

The Ulam metric is particularly interesting because:
- It's harder to work with than Hamming or Levenshtein distances
- Optimal constructions are unknown for most parameters
- It has connections to sorting algorithms and ranking problems

## Results Achieved

For **U(7,3)**:
- equiv_d3 typically finds: **56 permutations**
- improve_pa can reach: **59-64 permutations**
- **New record**: 64 permutations (beating previous best of 59!)

This demonstrates that the combination of structured construction followed by local search can be very effective for this problem.

## Future Directions

Potential improvements to explore:

1. **Parallelization**: Run multiple improve_pa instances with different seeds
2. **Adaptive parameters**: Automatically tune kick sizes based on success rate
3. **Hybrid methods**: Combine equivalence classes with other structures
4. **Larger n**: Extend to n=8,9,... (computational challenge increases rapidly)
5. **Other distances**: Adapt methods for d=4,5,...

## Contributing

When experimenting with these programs:
- Document your parameter choices
- Save all results (both successful and unsuccessful)
- Note any patterns or insights
- Share improvements to the algorithms

## References

- Ulam distance: Based on work in combinatorics and ranking theory
- Permutation arrays: Related to error-correcting codes
- Local search: Classical metaheuristic optimization technique

## Contact

For questions about these implementations or to share results, please contact the research team.

---

**Note**: Both programs include extensive inline comments explaining the algorithms. Reading the source code is highly recommended for understanding the implementation details.
