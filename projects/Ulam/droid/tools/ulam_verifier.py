#!/usr/bin/env python3
import sys
import json

# ---------------------------------------------
# LCS computation (Longest Common Subsequence)
# ---------------------------------------------
def lcs(a, b):
    n = len(a)
    dp = [[0] * (n + 1) for _ in range(n + 1)]
    for i in range(1, n + 1):
        for j in range(1, n + 1):
            if a[i - 1] == b[j - 1]:
                dp[i][j] = dp[i - 1][j - 1] + 1
            else:
                dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
    return dp[n][n]

# ---------------------------------------------
# Ulam Distance = n - LCS
# ---------------------------------------------
def ulam_distance(a, b):
    return len(a) - lcs(a, b)

# ---------------------------------------------
# Check if candidate is valid for the array
# ---------------------------------------------
def is_valid_candidate(candidate, array, minDist):
    for perm in array:
        if ulam_distance(candidate, perm) < minDist:
            return False
    return True

# ---------------------------------------------
# Extend array with a list of candidates
# ---------------------------------------------
def extend_array(array, candidates, minDist):
    accepted = []
    for c in candidates:
        if is_valid_candidate(c, array, minDist):
            accepted.append(c)
    return accepted

# ---------------------------------------------
# MAIN: Read JSON from stdin and return JSON
# ---------------------------------------------
def main():
    try:
        data = json.load(sys.stdin)
    except Exception as e:
        print(json.dumps({"error": f"Invalid JSON input: {str(e)}"}))
        return

    mode = data.get("mode")

    # ------------------------------
    # MODE: distance
    # ------------------------------
    if mode == "distance":
        a = data["a"]
        b = data["b"]
        dist = ulam_distance(a, b)
        print(json.dumps({"distance": dist}))
        return

    # ------------------------------
    # MODE: validate
    # ------------------------------
    if mode == "validate":
        candidate = data["candidate"]
        array = data["array"]
        minDist = data["minDist"]
        valid = is_valid_candidate(candidate, array, minDist)
        print(json.dumps({"valid": valid}))
        return

    # ------------------------------
    # MODE: extend
    # ------------------------------
    if mode == "extend":
        candidates = data["candidates"]
        array = data["array"]
        minDist = data["minDist"]
        accepted = extend_array(array, candidates, minDist)
        print(json.dumps({"accepted": accepted}))
        return

    # ------------------------------
    # Unknown mode
    # ------------------------------
    print(json.dumps({"error": f"Unknown mode '{mode}'"}))


if __name__ == "__main__":
    main()
