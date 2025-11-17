# Potential Undiscovered Theorems in Ulam Distance Research

## Analysis Date: 2025-11-17

Based on analysis of experimental data in the Ulam distance codebase, here are potential theorems that warrant further investigation and formal proof:

---

## 1. **Nested Interleaving Amplification Theorem**

### Observation:
From `ulam_data.csv`, when comparing nested interleavings of two permutations A and A':
- When base_d = 1, nested interleaving produces new_D ranging from 8-9
- The ratio jumps to 4.0-4.5× the base distance
- Simple interleaving only produces 2.0-2.5× amplification

### Conjecture:
**For permutations A and A' with Ulam distance d, recursive nested interleaving produces an Ulam distance ≥ k·d where k ≈ 4 for depth-2 nesting.**

Formally: If `NestedInterleave(A, A', depth=k)` denotes k levels of interleaving, then:
```
d_U(NestedInterleave_k(A, A')) ≥ 2^k · d_U(A, A')
```

### Evidence:
- All cases with nested interleaving show ≥ 4× amplification
- Pattern appears consistent regardless of base permutation structure
- Suggests exponential growth with nesting depth

---

## 2. **Fixed-Core Permutation Array Bound**

### Observation:
From `fixed_core_n100_d5.txt` and PA results:
- For PA(100, 5), achieved size = 43 permutations
- Pattern shows last 5 elements form specific "fixed core" patterns
- The core consistently ends with patterns like: `[99, 98, 100, 97, 96]`

### Conjecture:
**For PA(n, d) where d ≪ n, there exists a construction with a "fixed core" of size ≈ d yielding:**
```
|PA(n, d)| ≥ ⌊n/d⌋ + O(log d)
```

### Supporting Pattern:
Looking at the fixed-core results:
- n=100, d=5: size=43 ≈ 100/5 + 23
- The fixed suffix strategy appears to preserve minimum distance
- Rotational shifts in the first n-d elements maintain distance bounds

---

## 3. **Interleaving Distance Preservation under Rotation**

### Observation:
From `ulam_results.txt` and `ulam_data.csv`:
- `Interleave(A, rotate(A',1))` vs `Interleave(rotate(A',1), A)` maintains high distances
- Ratio typically 2.0-2.5× regardless of base distance
- Rotation parameter doesn't significantly affect the distance multiplier

### Conjecture:
**For any permutation A and rotation A' = rotate(A, k):**
```
d_U(Interleave(A, A')) ≈ d_U(Interleave(A, rotate(A', m))) 
```
**for any rotation amount m, where the difference is bounded by O(√n).**

### Evidence:
All rotation-based interleavings in the data show consistent 4-5 distance regardless of rotation amount.

---

## 4. **Cyclic Group Code Construction Lower Bound**

### Observation:
From `UlamCodeBuilder.java` and `UlamInterleaver.java`:
- Theorem 17 guarantees d_U(C) ≥ 2t+1 for s ≥ 4t+1
- Experimental results consistently meet this bound
- Code size is exactly s^(2t+1) as expected

### Potential Strengthening:
**The bound 2t+1 may not be tight. Evidence suggests:**
```
d_U(C) ≥ max(2t+1, ⌈s/2⌉) when s ≥ 4t+1
```

### Supporting Evidence:
- For large s relative to t, distances appear to grow
- When s=9, t=2, observed distances exceed 5 (the 2t+1 bound)
- Suggests additional structure in cyclic group construction

---

## 5. **Reverse-Concatenation Distance Symmetry**

### Observation:
From `ulam_results.txt`:
- `A|reverse(A')` vs `reverse(A)|A'` shows distance 4
- `A'|reverse(A)` vs `reverse(A')|A` shows distance 5
- Pattern differs based on which permutation is reversed first

### Conjecture:
**For permutations A and A' with d_U(A, A') = d:**
```
|d_U(A|rev(A'), rev(A)|A') - d_U(A'|rev(A), rev(A')|A)| ≤ 1
```

**And both distances are bounded by:**
```
2d ≤ d_U(A|rev(A'), rev(A)|A') ≤ n - d
```

---

## 6. **Maximum Distance Permutation Pair Theorem**

### Observation:
From csv data, when base_d = 3 (highest in dataset):
- Ratio drops to 0.5-1.0 for most constructions
- Reverse-full permutation `[4,3,2,1]` vs `[1,2,3,4]` achieves d=3 for n=4
- This appears to be maximal for n=4

### Conjecture:
**The maximum Ulam distance between permutations of length n is:**
```
d_U^max(n) = n - LIS_min(n)
```
**where LIS_min(n) is the minimum possible longest increasing subsequence for any permutation pair.**

For n=4: max distance = 3 (achieved by reverse permutations)

### Open Question:
What is the exact formula for d_U^max(n)? Current data suggests:
- n=4: max=3
- Conjecture: max ≈ ⌊2n/3⌋ or ⌊n - √n⌋

---

## 7. **Interleaving Non-Commutativity Bound**

### Observation:
Throughout `ulam_data.csv`:
- `Interleave(A, A')` vs `Interleave(A', A)` consistently shows distance 4-5
- Never equal (non-commutative operation)
- Ratio to base distance is consistently 2.0-2.5×

### Conjecture:
**The Ulam distance between commuted interleavings is bounded:**
```
2·d_U(A, A') ≤ d_U(Interleave(A,A'), Interleave(A',A)) ≤ 3·d_U(A, A')
```

**Moreover, the non-commutativity distance is at least:**
```
d_U(Interleave(A,A'), Interleave(A',A)) ≥ min(n/2, 2·d_U(A,A'))
```

---

## Recommended Next Steps:

1. **Formal Proof Attempts:**
   - Start with Nested Interleaving Amplification (appears most regular)
   - Use inductive approach on nesting depth

2. **Additional Experiments:**
   - Test nested interleaving with depth k=3,4,5
   - Verify 2^k growth hypothesis
   - Test with larger n values (n=50, 100)

3. **Computational Verification:**
   - Enumerate all permutations for n=5,6,7 to verify max distance conjecture
   - Check if fixed-core construction generalizes to other d values

4. **Literature Review:**
   - Check if nested interleaving bounds are known
   - Verify if fixed-core construction relates to known PA constructions
   - Compare with existing error-correcting code constructions

---

## Most Promising for New Theorems:

**Primary candidates** (strongest evidence):
1. Nested Interleaving Amplification (Theorem 1) - Very regular pattern
2. Interleaving Distance Preservation under Rotation (Theorem 3) - Consistent data

**Secondary candidates** (need more data):
3. Fixed-Core PA Bound (Theorem 2) - Single large example, needs more data points
4. Cyclic Group Code strengthening (Theorem 4) - May improve Theorem 17

**Speculative** (interesting but less evidence):
5. Maximum Distance formula (Theorem 6) - Only small n tested
6. Reverse-Concatenation Symmetry (Theorem 5) - Limited examples
7. Non-commutativity bound (Theorem 7) - Narrow bound range observed
