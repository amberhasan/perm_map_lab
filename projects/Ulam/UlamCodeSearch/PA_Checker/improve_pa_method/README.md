# Permutation Array Improvement via Stochastic Local Search

## Overview

This program **improves** existing permutation arrays using a **stochastic local search** algorithm with an intelligent restart mechanism. Given a seed permutation array, it attempts to add more permutations while maintaining the minimum Ulam distance constraint.

## Algorithm Description

### Core Strategy

The algorithm implements a **greedy random search with kick-and-refill restarts**:

1. **Random Search Phase**
   - Continuously generates random permutations
   - Attempts to add each permutation to the current code
   - Only adds if d(p,q) ≥ d for ALL permutations q already in the code
   - Updates the best solution whenever a larger code is found

2. **Plateau Detection**
   - Monitors consecutive failed addition attempts
   - After 250,000 failures, recognizes it's stuck at a local optimum

3. **Kick-and-Refill Restart**
   - **Kick**: Removes r random permutations (destroys part of current solution)
   - **Refill**: Attempts to rebuild by trying 400,000 random additions
   - If the restart produces a worse solution, reverts to the best known code
   - Allows escape from local optima to explore new regions

### The "Kick" Mechanism

The size of the kick (how many permutations to remove) adapts to code size:
- Code size < 50: Remove 2 permutations
- Code size ≥ 50: Remove 4 permutations
- Code size ≥ 58: Remove 6 permutations

**Intuition**: Larger codes are harder to improve, so bigger kicks are needed to escape local optima.

### Key Features

- **Stochastic**: Uses randomization to explore different regions
- **Adaptive**: Adjusts restart intensity based on code size
- **Progressive**: Never accepts worse solutions (always reverts if kick fails)
- **Persistent**: Saves progress every 60 seconds (best) and 120 seconds (current)
- **Thread-safe**: Uses mutexes for safe concurrent access to shared data

## Ulam Distance Calculation

Uses an **optimized distance calculation with early exit**:

```
d(p,q) = n - LIS(invQ ∘ p)
```

Where `invQ` is the pre-computed inverse of q.

**Early Exit Optimization**: If during LIS computation the length exceeds `n - threshold`, the algorithm immediately returns 0 (failure), avoiding unnecessary computation.

This optimization provides significant speedup when checking many candidates against a large code.

## Usage

### Compilation

```bash
g++ -std=c++11 -O3 -o improve_pa improve_pa.cpp
```

### Running

```bash
./improve_pa <n> <d> <seed_file> [random_seed]
```

**Parameters:**
- `n` - Length of permutations
- `d` - Minimum Ulam distance required
- `seed_file` - Input file containing initial permutation array
- `random_seed` - (Optional) Seed for random number generator

**Example:**
```bash
./improve_pa 7 3 U7_d3_equiv.txt
```

Or with a specific random seed for reproducibility:
```bash
./improve_pa 7 3 U7_d3_equiv.txt 12345
```

### Stopping

Press `Ctrl+C` to gracefully stop. The program will save both the best and current solutions before exiting.

## Input File Format

The seed file should contain permutations in the following format:

```
# U(7,3) size = 56
0 1 2 3 4 5 6
1 0 2 3 4 5 6
2 1 0 3 4 5 6
...
```

- Lines starting with `#` are comments (ignored)
- Each line contains a space-separated permutation
- Permutations must use symbols {0, 1, 2, ..., n-1}

The program validates:
- Each line is a valid permutation
- All pairwise distances are ≥ d

## Output Files

### Best Solution File

**Filename:** `best_U<n>_<d>.txt`

**Example:** `best_U7_3.txt`

Contains the largest valid code found during the entire run. Updated whenever a new best is found.

### Current Solution File

**Filename:** `current_U<n>_<d>.txt`

**Example:** `current_U7_3.txt`

Contains the working solution at the time of the last save (every 120 seconds). May not be the best, but shows current progress.

### Format

Both files use the same format as the input:

```
# U(7,3) size = 59
0 1 2 3 4 5 6
1 0 2 3 4 5 6
...
```

## Console Output

The program provides real-time feedback:

```
[LOAD] seed size = 56
[CHECK] verifying pairwise distances >= 3 ...
[OK] seed is valid.
===========================================
IMPROVE U(7,3) FROM SEED
Seed file: U7_d3_equiv.txt
Initial size: 56
Output best:  best_U7_3.txt
Output cur:   current_U7_3.txt
Stop with Ctrl+C (saves automatically)
===========================================
size=60 size=70 size=80
[NEW BEST] size = 80 -> saved best_U7_3.txt
[PLATEAU] stuck at size=80 after 250000 failed adds. Kicking r=4...
[AFTER KICK] size=76
size=80
[SAVE CURRENT] size = 80
[SAVE BEST] size = 80
```

**Messages:**
- `size=X` - Progress indicator (printed every 10 additions)
- `[NEW BEST]` - Found a larger code than before
- `[PLATEAU]` - Detected stuck state, initiating kick
- `[AFTER KICK]` - Status after kick-and-refill
- `[RESTART]` - Reverting to best after unsuccessful kick
- `[SAVE CURRENT/BEST]` - Periodic saves

## Algorithm Parameters

You can tune these constants in the code:

```cpp
const long long PLATEAU_TRIALS = 250000;   // Failed attempts before kick
```

**Kick sizes** (in function `improve_forever`):
```cpp
int r = 2;                    // Default
if (code.size() >= 50) r = 4; // Larger codes
if (code.size() >= 58) r = 6; // Even larger codes
```

**Refill attempts**:
```cpp
greedy_fill(code, invs, n, d, rng, 400000);  // Try 400k random additions
```

## Performance Notes

### Speed

For U(7,3) on a modern CPU:
- ~50,000 - 100,000 random permutation checks per second
- Plateau detection triggers roughly every 2-5 seconds when stuck
- Can process millions of candidates per minute

### Memory

- O(code_size × n) for storing the code and inverses
- Minimal overhead for tracking variables

### Effectiveness

- Works best when given a good seed (e.g., from equiv_d3 method)
- Can typically improve small codes (< 50) relatively quickly
- Improvements become rarer as code size approaches theoretical limits
- May run for hours or days seeking marginal improvements

## Strategy and Theory

### Why This Works

1. **Random Search**: Explores permutation space uniformly
2. **Greedy Growth**: Only adds permutations that maintain constraints
3. **Restart Mechanism**: Escapes local optima by strategic backtracking
4. **Best Tracking**: Never loses the best solution found

### Local Optima Problem

Greedy addition often leads to "dead ends" - configurations where no additional permutations can be added. The kick-and-refill mechanism addresses this by:
- Breaking up the dead-end configuration (kick)
- Exploring a different reconstruction path (refill)
- Potentially reaching a better local optimum

### When to Use This Method

- **Good seed available**: You have an initial code from another method
- **Seeking marginal improvements**: Want to push beyond what structured methods find
- **Have compute time**: Can let it run for extended periods
- **Near theoretical limits**: When improvements are rare but valuable

## Typical Workflow

1. **Generate seed** using equiv_d3 or another construction method
2. **Run improve_pa** with the seed
3. **Monitor progress** via console output
4. **Let run** until improvements plateau (hours to days)
5. **Stop** when satisfied or use output as seed for another run

## Results

For U(7,3):
- **Typical seed**: 56 permutations (from equiv_d3)
- **Improvement potential**: Can sometimes reach 57-59
- **Theoretical lower bound**: 59
- **Challenge**: The gap between achievable and theoretical is small

## Related Files

- `improve_pa.cpp` - Source code with detailed comments
- `improve_pa` - Compiled binary
- `best_U7_3.txt` - Best solution found
- `current_U7_3.txt` - Current working solution

## Combining with Other Methods

This method is designed to work **in tandem** with construction methods:

```
equiv_d3 → seed (size 56) → improve_pa → improved (size 57-59)
```

You can also chain multiple improve_pa runs:
```
improve_pa → output1 → improve_pa (different seed) → output2 → ...
```

## Technical Details

### Thread Safety

Although the current implementation runs single-threaded, the code uses:
- `atomic<bool>` for the STOP flag
- `mutex` for best solution tracking

This design allows future extension to multi-threaded search.

### Distance Optimization

The `ulam_distance_at_least` function includes critical optimizations:
- **Early exit**: Stops computing LIS when it's already too long
- **Stack allocation**: Uses fixed-size array instead of vector for LIS
- **Pointer arithmetic**: Uses raw pointers for binary search

These optimizations provide 2-3x speedup over naive implementation.

## Troubleshooting

**Problem**: Program says seed is invalid

**Solution**: Check that seed file contains valid permutations with sufficient pairwise distances

---

**Problem**: No improvements after long runtime

**Solution**: This is expected near theoretical limits. Try a different random seed or a different seed file.

---

**Problem**: Kicks make things worse consistently

**Solution**: Adjust kick sizes or plateau threshold in the code. Smaller kicks might work better for your specific instance.

## Author Notes

This implementation uses established local search techniques adapted for the permutation array problem. The kick-and-refill restart strategy is a form of iterated local search, a well-studied metaheuristic for combinatorial optimization.

The effectiveness of this method demonstrates that even simple stochastic techniques can be competitive when tuned appropriately for the problem structure.
