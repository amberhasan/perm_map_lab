# FracSearchFC

`FracSearchFC` is a Java program that searches for **Normalized Fractional Permutation Polynomials (NFPPs)** over finite fields GF(pⁿ), where the rational function f(x)/g(x) permutes the elements of the field.

It allows you to:

- Specify a finite field GF(pⁿ)
- Define the degrees of f(x) and g(x)
- Fix coefficients of the numerator polynomial to specific values or force them to zero
- Search for unique NFPPs modulo common transformations

---

## ⚙️ How to Compile

Make sure all `.java` files are in the same directory:

```bash
javac FracSearchFC/FracSearchFC.java
java FracSearchFC.FracSearchFC 3 2 4 2 2 0 3 1 1 2
```

This will also compile `GF.java` and `Polynomial.java` automatically if they are in the same file.

---

## ▶️ How to Run

```bash
java FracSearchFC <prime> <power> <f-degree> <g-degree> [<degree> <value>]... [-v]
```

---

## 🔷 Required Arguments

```text
<prime>      // Prime number p for the field GF(p^n)
<power>      // Exponent n for GF(p^n)
<f-degree>   // Degree of the numerator polynomial f(x)
<g-degree>   // Degree of the denominator polynomial g(x)
```

**Note:** `f-degree` must be strictly greater than `g-degree`.

---

## 🔷 Optional Arguments: Fixing Coefficients in f(x)

You can pass extra arguments as **pairs** of `<degree> <value>` after the required four inputs:

```text
<degree> <value>
```

- If `<value>` = `0` → sets the coefficient of `x^<degree>` in f(x) to **0**
- If `<value>` ≠ `0` → sets that coefficient to the given non-zero value

You can chain multiple pairs.

Examples:

```text
2 0     // fixes coefficient of x^2 to 0
3 1     // fixes coefficient of x^3 to 1
1 2     // fixes coefficient of x^1 to 2
```

Full command example:

```bash
java FracSearchFC 3 2 4 2 2 0 3 1 1 2
```

---

## 🔷 Optional Flag

```text
-v      // Enables verbose mode (prints all NFPPs found to the console)
```

Example with verbose mode:

```bash
java FracSearchFC 3 2 4 2 2 0 3 1 1 2 -v
```

---

## 📤 Output

- **Console:**

  - Progress updates: percentage complete, time elapsed, estimated time remaining
  - Number of NFPPs found so far
  - If `-v` is used, prints each NFPP to the console

- **File:**

  - All NFPPs are written to a file named:

    ```
    frac_<prime>_<power>_<f-degree>_<g-degree>.txt
    ```

    Example: `frac_3_2_4_2.txt`

---

## 📚 Description

This program performs an exhaustive search over candidate numerator and denominator polynomials in GF(pⁿ), using:

- **Bitmasking** to generate valid monomials
- **f-map**, **g-map**, and **f(x+b)** equivalence transformations to deduplicate NFPPs
- **Polynomial GCDs** to ensure f(x) and g(x) are coprime
- **Field arithmetic** using lookup tables generated for GF(pⁿ)
- **Irreducibility and primitiveness** checks for field construction

It uses optimization strategies like:

- Locking certain coefficients to reduce search space
- Skipping polynomials that would lead to division by zero

---

## 🧠 Example Use Case

You want to find all NFPPs of the form f(x)/g(x) in GF(3²), where:

- deg(f) = 4
- deg(g) = 2
- f(x) has coefficient 0 at x²
- f(x) has coefficient 1 at x³
- f(x) has coefficient 2 at x¹
- and you want verbose output

Then you'd run:

```bash
java FracSearchFC 3 2 4 2 2 0 3 1 1 2 -v
```

---

## 👨‍🔬 Research Context

This tool was developed as part of research into fractional permutation polynomials. It builds on prior work in finite fields, polynomial algebra, and combinatorics.

Originally developed under the guidance of Dr. Sudborough, who is known for contributions in combinatorics, graph theory, and complexity theory — including improving bounds in the **pancake sorting problem** once held by Bill Gates.

---

## 🧼 Output Cleanup Tip

Each time you run the program, it overwrites the output file `frac_<prime>_<power>_<f-degree>_<g-degree>.txt`. If you want to retain previous runs, rename or back them up before re-running.

---

## 📎 Dependencies

- Pure Java (no external libraries required)
- Java 8 or later

---

## 🛠️ Troubleshooting

- **Error: `Could not find or load main class`**  
  Make sure you're in the same directory where the `.class` file is and use the correct case:

  ```bash
  java FracSearchFC ...
  ```

- **Program seems stuck or slow**  
  You may be running with a large search space. Try lowering `f-degree` and `g-degree`, or fix more coefficients to speed it up.

---

## 📩 Contact

For academic/research inquiries or further enhancements, please reach out to the project maintainer.
