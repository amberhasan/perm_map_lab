import java.io.*;
import java.util.*;

/**
 * Greedy construction of a maximum Ulam-distance-2 permutation array U(n,2).
 *
 * Algorithm:
 *   S = all permutations of {1..n}
 *   A = empty
 *
 *   while S not empty:
 *       pick x in S with d_U(x, a) >= 2 for all a in A
 *       add x to A
 *       remove x from S
 *       remove all permutations obtained by moving symbol 1 in x
 *
 * For n=7, |A| should be (n-1)! = 720.
 */
public class Ulam7Greedy {

    static int n = 7;

    public static void main(String[] args) throws Exception {
        List<int[]> S = generateAllPermutations(n);
        List<int[]> A = new ArrayList<>();

        System.out.println("Initial |S| = " + S.size());

        while (!S.isEmpty()) {
            int[] x = null;

            // Find a valid x
            for (int[] p : S) {
                if (farFromAll(p, A)) {
                    x = p;
                    break;
                }
            }

            if (x == null) {
                break; // no valid extension
            }

            A.add(x);

            // Remove x and all Ulam-distance-1 neighbors (via symbol 1)
            removeNeighbors(S, x);

            if (A.size() % 50 == 0) {
                System.out.println("A size = " + A.size() + ", S size = " + S.size());
            }
        }

        System.out.println("\nDONE");
        System.out.println("Final |A| = " + A.size());
        System.out.println("Expected = " + factorial(n - 1));

        writeToFile(A, "U7_d2.txt");
        System.out.println("Wrote permutations to U7_d2.txt");

    }

    /* ------------------------------------------------------------ */

    // Check Ulam distance >= 2 from all permutations in A
    static boolean farFromAll(int[] p, List<int[]> A) {
        for (int[] q : A) {
            if (ulamDistance(p, q) < 2) {
                return false;
            }
        }
        return true;
    }

    // Remove x and all permutations obtained by moving symbol 1
    static void removeNeighbors(List<int[]> S, int[] x) {
        int pos1 = indexOf(x, 1);

        Iterator<int[]> it = S.iterator();
        while (it.hasNext()) {
            int[] y = it.next();

            if (Arrays.equals(y, x)) {
                it.remove();
                continue;
            }

            int pos1y = indexOf(y, 1);

            // Check if y is x with symbol 1 moved
            if (sameExceptOneMove(x, y, pos1, pos1y)) {
                it.remove();
            }
        }
    }

    // Check whether y is obtained by moving symbol 1 in x
    static boolean sameExceptOneMove(int[] x, int[] y, int i, int j) {
        if (i == j) return false;

        int idx = 0;
        for (int k = 0; k < x.length; k++) {
            if (k == i) continue;
            if (y[idx] != x[k]) return false;
            idx++;
            if (idx == j) idx++; // skip the inserted 1
        }
        return y[j] == 1;
    }

    /* ------------------------------------------------------------ */

    // Ulam distance = n - LCS length
    static int ulamDistance(int[] a, int[] b) {
        int[] pos = new int[n + 1];
        for (int i = 0; i < n; i++) {
            pos[b[i]] = i;
        }

        int[] seq = new int[n];
        for (int i = 0; i < n; i++) {
            seq[i] = pos[a[i]];
        }

        return n - lisLength(seq);
    }

    // Longest Increasing Subsequence (O(n^2), fine for n=7)
    static int lisLength(int[] a) {
        int[] dp = new int[a.length];
        int best = 0;

        for (int i = 0; i < a.length; i++) {
            dp[i] = 1;
            for (int j = 0; j < i; j++) {
                if (a[j] < a[i]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            best = Math.max(best, dp[i]);
        }
        return best;
    }

    /* ------------------------------------------------------------ */

    static int indexOf(int[] a, int x) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == x) return i;
        }
        return -1;
    }

    static List<int[]> generateAllPermutations(int n) {
        List<int[]> res = new ArrayList<>();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i + 1;
        permute(a, 0, res);
        return res;
    }

    static void permute(int[] a, int i, List<int[]> res) {
        if (i == a.length) {
            res.add(a.clone());
            return;
        }
        for (int j = i; j < a.length; j++) {
            swap(a, i, j);
            permute(a, i + 1, res);
            swap(a, i, j);
        }
    }

    static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    static int factorial(int x) {
        int r = 1;
        for (int i = 2; i <= x; i++) r *= i;
        return r;
    }

    static void writeToFile(List<int[]> A, String filename) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            for (int[] p : A) {
                for (int i = 0; i < p.length; i++) {
                    out.print(p[i]);
                    if (i + 1 < p.length) out.print(" ");
                }
                out.println();
            }
        }
    }

}
