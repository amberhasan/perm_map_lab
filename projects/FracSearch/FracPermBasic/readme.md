# FracPermExplorer

FracPermExplorer is a simple brute-force tool to search for **fractional permutation polynomials (FPPs)** over finite fields of the form: f(x)/g(x)

It checks whether this rational function defines a **permutation** over the finite field GF(p^n), and optionally whether `f(x)/g(x) + x` is also a permutation.

## 🔍 What It Does

- Enumerates all monic numerator polynomials `f(x)` of degree `fdegree`
- Enumerates all monic denominator polynomials `g(x)` of degree `gdegree`
- Skips `g(x)` if it evaluates to 0 anywhere in GF (to avoid division by 0)
- Ensures `f(x)` and `g(x)` are coprime (GCD = 1)
- Evaluates `f(x)/g(x)` for all `x` in GF
- Checks whether:
  - The resulting array is a **permutation** of GF
  - The array plus `x` is also a permutation

## 📥 How to Use

### Compile

```bash
javac FracHal.java
```

### Run

`java FracHal <prime> <power>`

### Output

The console will display:

The f(x)/g(x) values

Whether it is a permutation

Whether f(x)/g(x) + x is a permutation

A final count of total permutations found

### Example Output

[1, 0, 1, 2] / [1, 2, 2] = [4, 0, 1, 7, 6, 8, 2, 3, 5] PERMUTATION
[1, 0, 1, 2] / [1, 2, 2] + x = [4, 1, 3, 1, 1, 2, 8, 1, 4] +X PERMUTATION

### ⚙️ Default Settings

fdegree = 3

gdegree = 2

Numerator and denominator polynomials are always monic (leading coefficient = 1)

No coefficient fixing or normalization

### 🧪 Use Cases

Explore small finite fields for fractional permutation polynomials

Check if adding x preserves permutation properties

Great for prototyping and generating data

### 🚫 Limitations

No normalization/equivalence class detection (use FracSearch for that)

Limited to small fields due to brute-force nature

Hardcoded degrees unless modified in source
