# Tool: ulam_verifier

This tool checks Ulam distance constraints for permutations.
It is used by Droid agents to verify candidate permutations, compute Ulam distance, and extend permutation arrays under a minimum Ulam distance requirement.

Run command (STDIN → STDOUT):

```
python3 droid/tools/ulam_verifier.py
```

The tool accepts **JSON input** from STDIN and returns **JSON output** to STDOUT.

---

## Supported Modes

The tool supports three modes:

---

## 1. Compute Ulam Distance

### Input

```
{
  "mode": "distance",
  "a": [1, 2, 3, 4],
  "b": [1, 3, 4, 2]
}
```

### Output

```
{
  "distance": 2
}
```

---

## 2. Validate a Candidate Permutation

Checks whether a candidate permutation maintains  
`ulam_distance >= minDist` with all permutations in the existing array.

### Input

```
{
  "mode": "validate",
  "candidate": [1, 3, 4, 2],
  "array": [[1, 2, 3, 4], [4, 3, 2, 1]],
  "minDist": 2
}
```

### Output

```
{
  "valid": true
}
```

---

## 3. Extend Array With Multiple Candidates

Filters a list of candidate permutations, returning only those that satisfy
the minimum Ulam distance requirement.

### Input

```
{
  "mode": "extend",
  "candidates": [[1, 3, 4, 2], [3, 1, 2, 4]],
  "array": [[1, 2, 3, 4], [4, 3, 2, 1]],
  "minDist": 2
}
```

### Output

```
{
  "accepted": [[1, 3, 4, 2]]
}
```

---

## Notes

- All permutations must have the same length.
- The integers in each permutation must form a valid permutation.
- The tool outputs **only JSON** (no extra console text).
