import itertools
from functools import lru_cache

# ==============================
#  Ulam distance utilities
# ==============================

# LCS on permutations using a DP table
@lru_cache(None)
def lcs(a, b):
    n = len(a)
    dp = [[0] * (n+1) for _ in range(n+1)]
    for i in range(n):
        for j in range(n):
            if a[i] == b[j]:
                dp[i+1][j+1] = dp[i][j] + 1
            else:
                dp[i+1][j+1] = max(dp[i][j+1], dp[i+1][j])
    return dp[n][n]


def ulam_distance(p, q):
    return len(p) - lcs(tuple(p), tuple(q))


# ==============================
#  Branch & bound for U(n, d)
# ==============================

best_code = []  # stores best solution globally

def can_add(code, new_perm, d):
    """Check Ulam distance constraint to all existing codewords."""
    for p in code:
        if ulam_distance(p, new_perm) < d:
            return False
    return True


def search(candidates, code, d):
    """Branch-and-bound recursion."""
    global best_code

    # Too small to beat best (prune)
    if len(code) + len(candidates) <= len(best_code):
        return

    # If no candidates, record solution
    if not candidates:
        if len(code) > len(best_code):
            best_code = code.copy()
        return

    # Explore
    for i, pi in enumerate(candidates):
        if can_add(code, pi, d):
            newC = code + [pi]
            newCand = [x for x in candidates[i+1:] if can_add(newC, x, d)]
            search(newCand, newC, d)

    # Optionally skip branch (no need to explicitly track)


# ==============================
#  Main driver
# ==============================

def compute_U(n, d, order="lex"):
    """
    Compute U(n, d) = largest size of permutation code with Ulam distance >= d.
    Order can be: "lex", "random", or sorted by heuristic later.
    """
    global best_code
    best_code = []

    perms = list(itertools.permutations(range(1, n+1)))

    # Symmetry break: always include identity as first codeword
    id_perm = tuple(range(1, n+1))
    perms.remove(id_perm)
    candidates = perms

    search(candidates, [id_perm], d)
    return best_code


# ==============================
#  Example run
# ==============================

if __name__ == "__main__":
    n = 8
    d = 5
    result = compute_U(n, d)
    print(f"U({n}, {d}) = {len(result)}")
    for r in result:
        print(r)
