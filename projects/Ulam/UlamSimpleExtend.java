// UlamSimpleExtend.java
// Minimal demo: extend a (4,2) Ulam PA to (5,3) by inserting 5 in all positions.
// Uses standard LCS DP to compute Ulam distance.
//So, we are adding a new n+1 element to every spot on each of the base PA's, then only keeping the PA's with the ulam distance of d+1 or more.

import java.util.*;

public class UlamSimpleExtend {

    // --------- Ulam distance using LCS ---------
    static int ulamDistance(int[] perm1, int[] perm2) {
        // Encode permutations as strings of letters (A, B, C, …)
        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        for (int value : perm1) s1.append((char) ('A' + value));
        for (int value : perm2) s2.append((char) ('A' + value));

        int lcsLength = longestCommonSubsequence(s1.toString(), s2.toString());
        return perm1.length - lcsLength;
    }

    // Standard LCS DP
    public static int longestCommonSubsequence(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = a.length() - 1; i >= 0; i--) {
            for (int j = b.length() - 1; j >= 0; j--) {
                if (a.charAt(i) == b.charAt(j)) {
                    dp[i][j] = dp[i + 1][j + 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }
        return dp[0][0];
    }

    static boolean isValidExtension(List<int[]> currentArray, int[] candidate, int minDistance) {
        for (int[] existing : currentArray) {
            if (ulamDistance(existing, candidate) < minDistance) return false;
        }
        return true;
    }

    // --------- Insert new symbol at all positions ---------
    static List<int[]> insertSymbolAllPositions(int[] permutation, int newSymbol) {
        int n = permutation.length;
        List<int[]> results = new ArrayList<>();
        for (int pos = 0; pos <= n; pos++) {
            int[] extended = new int[n + 1];
            System.arraycopy(permutation, 0, extended, 0, pos);
            extended[pos] = newSymbol;
            System.arraycopy(permutation, pos, extended, pos + 1, n - pos);
            results.add(extended);
        }
        return results;
    }

    static List<int[]> extendByOneSymbol(List<int[]> baseArray, int newSize, int minDistance) {
        List<int[]> extendedArray = new ArrayList<>();
        for (int[] perm : baseArray) {
            for (int[] candidate : insertSymbolAllPositions(perm, newSize)) {
                if (isValidExtension(extendedArray, candidate, minDistance)) {
                    extendedArray.add(candidate);
                }
            }
        }
        return extendedArray;
    }

    // Simple demo to extend a (4,2) Ulam PA to (5,3)
    public static void main(String[] args) {
        // Base PA: (n=4, d=2)
        List<int[]> baseArray = new ArrayList<>();
        baseArray.add(new int[]{1, 2, 3, 4});
        baseArray.add(new int[]{4, 3, 2, 1});

        System.out.println("Base PA (n=4, d=2):");
        for (int[] perm : baseArray) System.out.println(Arrays.toString(perm));

        // Extend to n=5, d=3
        List<int[]> extendedArray = extendByOneSymbol(baseArray, 5, 3);

        System.out.println("\nExtended PA (n=5, d=3):");
        for (int[] perm : extendedArray) System.out.println(Arrays.toString(perm));
    }
}
