// UlamPA.java
// Build large permutation arrays (PAs) under the Ulam metric.
// Heuristics: random-greedy, lex-greedy; optional expansion from a base file.
// Ulam distance d_U(p,q) = n - LCS(p,q), and for permutations LCS = LIS(pos-map(p in q)).
//
// Compile: javac UlamPA.java
// Run examples:
//   java UlamPA --n 9 --d 3 --algo random --iters 200000 --seed 42 --out pa_n9_d3.txt
//   java UlamPA --n 10 --d 4 --algo lex --out pa_n10_d4.txt
//   java UlamPA --expand_from base_n8_d4.txt --n 10 --d 4 --out pa_n10_d4_expanded.txt
//
// Notes:
// - Heuristic search; exact P_U(n,d) grows intractable.
// - Every candidate is verified against current set with fast O(n log n) distance.
// - Expansion mirrors Chebyshev-style recursion but we still verify for Ulam.

import java.io.*;
import java.util.*;

public class UlamPA {

    // ---------------------- Ulam distance via LIS ----------------------
    // For permutations p, q of length n:
    // Build pos[q[v]] = index; map p to a[i] = pos[p[i]]; LIS(a) = LCS(p,q); d_U = n - LIS.

    static int ulamDistance(int[] p, int[] q) {
        int n = p.length;
        int[] pos = new int[n + 1];
        Arrays.fill(pos, -1);
        for (int i = 0; i < n; i++) pos[q[i]] = i;

        int[] tails = new int[n]; // patience sorting
        int len = 0;
        for (int i = 0; i < n; i++) {
            int v = pos[p[i]];
            int idx = Arrays.binarySearch(tails, 0, len, v);
            if (idx < 0) idx = -idx - 1;
            tails[idx] = v;
            if (idx == len) len++;
        }
        return n - len;
    }

    static boolean okAgainstAll(List<int[]> A, int[] cand, int d) {
        for (int[] p : A) {
            if (ulamDistance(p, cand) < d) return false;
        }
        return true;
    }

    // -------------------------- Utilities ------------------------------

    static boolean isPerm(int[] p) {
        int n = p.length;
        boolean[] seen = new boolean[n + 1];
        for (int v : p) {
            if (v < 1 || v > n || seen[v]) return false;
            seen[v] = true;
        }
        return true;
    }

    static List<int[]> readPerms(String path) {
        List<int[]> res = new ArrayList<>();
        if (path == null || path.isEmpty()) return res;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            for (String line; (line = br.readLine()) != null; ) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] tok = line.split("\\s+");
                int[] p = new int[tok.length];
                for (int i = 0; i < tok.length; i++) p[i] = Integer.parseInt(tok[i]);
                res.add(p);
            }
        } catch (IOException e) {
            System.err.println("ERROR reading " + path + ": " + e.getMessage());
        }
        return res;
    }

    static void writePerms(String path, List<int[]> A) {
        if (path == null || path.isEmpty()) return;
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path)))) {
            for (int[] p : A) {
                for (int i = 0; i < p.length; i++) {
                    if (i > 0) out.print(' ');
                    out.print(p[i]);
                }
                out.println();
            }
        } catch (IOException e) {
            System.err.println("ERROR writing " + path + ": " + e.getMessage());
        }
    }

    // ---------------------- Candidate generation -----------------------

    static void lexGreedyBuild(int n, int d, List<int[]> A, long limit) {
        int[] cur = new int[n];
        for (int i = 0; i < n; i++) cur[i] = i + 1;
        do {
            if (A.isEmpty() || okAgainstAll(A, cur, d)) {
                A.add(cur.clone());
                if (limit > 0 && A.size() >= limit) return;
            }
        } while (nextPermutation(cur));
    }

    // Standard next_permutation (lexicographic) on int[] (1..n)
    static boolean nextPermutation(int[] a) {
        int i = a.length - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = a.length - 1;
        while (a[j] <= a[i]) j--;
        swap(a, i, j);
        reverse(a, i + 1, a.length - 1);
        return true;
    }

    static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
    static void reverse(int[] a, int l, int r) { while (l < r) swap(a, l++, r--); }

    static void randomGreedyBuild(int n, int d, List<int[]> A, long iters, long seed) {
        int[] cur = new int[n];
        for (int i = 0; i < n; i++) cur[i] = i + 1;
        Random rng = new Random(seed);
        for (long t = 0; t < iters; t++) {
            shuffle(cur, rng);
            if (A.isEmpty() || okAgainstAll(A, cur, d)) {
                A.add(cur.clone());
            }
        }
    }

    static void shuffle(int[] a, Random rng) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            swap(a, i, j);
        }
    }

    // ---------------------------- Expansion ----------------------------

    static List<int[]> insertAllPositions(int[] v, int x) {
        int n = v.length;
        List<int[]> out = new ArrayList<>(n + 1);
        for (int pos = 0; pos <= n; pos++) {
            int[] t = new int[n + 1];
            System.arraycopy(v, 0, t, 0, pos);
            t[pos] = x;
            System.arraycopy(v, pos, t, pos + 1, n - pos);
            out.add(t);
        }
        return out;
    }

    static void expandFromSmaller(List<int[]> base, int n, int d, List<int[]> A) {
        if (base.isEmpty()) return;
        int m = base.get(0).length;
        if (m >= n) return;

        for (int[] p : base) {
            if (p.length != m || !isPerm(p)) {
                System.err.println("ERROR: base contains non-permutation or wrong length");
                return;
            }
        }

        for (int[] p0 : base) {
            List<int[]> level = insertAllPositions(p0, m + 1);
            for (int x = m + 2; x <= n; x++) {
                List<int[]> next = new ArrayList<>();
                for (int[] v : level) {
                    next.addAll(insertAllPositions(v, x));
                }
                level = next;
            }
            for (int[] cand : level) {
                if (cand.length != n || !isPerm(cand)) continue;
                if (okAgainstAll(A, cand, d)) {
                    // (Optional) also check against those we’ve just added
                    A.add(cand);
                }
            }
        }
    }

    // ----------------------------- CLI ---------------------------------

    static class Args {
        int n = -1;
        int d = -1;
        String algo = "random"; // random | lex
        long iters = 200000;
        long seed = 123456789L;
        String out = "";
        String expandFrom = "";
    }

    static void help() {
        System.err.println(
            "Usage:\n" +
            "  java UlamPA --n N --d D [--algo random|lex] [--iters I] [--seed S] [--out FILE]\n" +
            "  java UlamPA --expand_from base.txt --n N --d D [--out FILE]\n"
        );
    }

    static Args parse(String[] argv) {
        Args a = new Args();
        for (int i = 0; i < argv.length; i++) {
            String s = argv[i];
            boolean need = (i + 1 < argv.length);
            switch (s) {
                case "--n": if (need) a.n = Integer.parseInt(argv[++i]); else return null; break;
                case "--d": if (need) a.d = Integer.parseInt(argv[++i]); else return null; break;
                case "--algo": if (need) a.algo = argv[++i]; else return null; break;
                case "--iters": if (need) a.iters = Long.parseLong(argv[++i]); else return null; break;
                case "--seed": if (need) a.seed = Long.parseLong(argv[++i]); else return null; break;
                case "--out": if (need) a.out = argv[++i]; else return null; break;
                case "--expand_from": if (need) a.expandFrom = argv[++i]; else return null; break;
                case "--help": return null;
                default:
                    System.err.println("Unknown arg: " + s);
                    return null;
            }
        }
        if (a.n <= 0 || a.d <= 0) return null;
        return a;
    }

    // ----------------------------- Main --------------------------------

    public static void main(String[] args) {
        Args a = parse(args);
        if (a == null) {
            help();
            return;
        }
        if (a.d > a.n) {
            System.err.println("ERROR: d > n is not meaningful for Ulam distance.");
            return;
        }

        List<int[]> A = new ArrayList<>();

        if (a.expandFrom != null && !a.expandFrom.isEmpty()) {
            List<int[]> base = readPerms(a.expandFrom);
            if (base.isEmpty()) {
                System.err.println("ERROR: expansion base is empty or unreadable.");
                return;
            }
            int m = base.get(0).length;
            if (m >= a.n) {
                System.err.println("ERROR: base length >= target n.");
                return;
            }
            System.err.printf("Expanding %d base perms of length %d to length %d with min Ulam distance %d...%n",
                    base.size(), m, a.n, a.d);
            expandFromSmaller(base, a.n, a.d, A);
            System.err.printf("Built array of size %d%n", A.size());
        } else {
            System.err.printf("Building Ulam PA with n=%d, d=%d, algo=%s...%n", a.n, a.d, a.algo);
            if ("random".equalsIgnoreCase(a.algo)) {
                randomGreedyBuild(a.n, a.d, A, a.iters, a.seed);
            } else if ("lex".equalsIgnoreCase(a.algo)) {
                // Warning: lex scan explodes for n>10. Use small n or set a limit if needed.
                lexGreedyBuild(a.n, a.d, A, Long.MAX_VALUE);
            } else {
                System.err.println("ERROR: unknown algo: " + a.algo);
                return;
            }
            System.err.printf("Built array of size %d (greedy heuristic)%n", A.size());
        }

        if (a.out != null && !a.out.isEmpty()) {
            writePerms(a.out, A);
            System.err.printf("Wrote %d permutations to %s%n", A.size(), a.out);
        }
    }
}
