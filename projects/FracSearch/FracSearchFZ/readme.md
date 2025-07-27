There are various versions.
FracSearchFZ has fixed indexes as 0.

# FracSearchFZ

**FracSearchFZ** is a Java program designed to search for permutation polynomials of fractional form over finite fields using Hermite’s Criterion. It tests candidate polynomials of the form `f(x)/g(x)` over `GF(p^r)` with specified degrees and constraints, reporting which ones are valid permutation polynomials.

---

## 🔧 How to Compile and Run

### Requirements

- Java 8+
- Terminal / VSCode Terminal

### Compile and Run

If you're in the projects/FracSearch, then you run this:

```bash
javac FracSearchFZ/FracSearchFZ.java
java FracSearchFZ.FracSearchFZ 11 1 5 4 1
```

###What's Going On###
`java FracSearchFZ 11 1 5 4 1` is saying

prime = 11
power = 1
fdegree = 5
gdegree = 4
fixedZeroDegrees = [1]

### Other

Usage: java FracSearch <prime> <power> <f-degree> <g-degree> <options>
options:
-v verbose output of nFPPs

Finds Fractional Permutation Polynomials in the form f(x)/g(x)

Normalization:
GCD(f(x), g(x)) = 1
f(x) and g(x) are monic
f(0) = 0, g(0) != 0
if prime does not divide the degree of g(x), second coefficient of g(x) is 0
