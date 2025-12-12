import java.util.*;

public class UlamCodeBuilder {
    // ---------- Parameters ----------
    static int t;                   // choose t
    static int tracks;              // 2t+1
    static int s;                   // elements per track
    static int n;                   // total = s*tracks
    static int minTrackHam;         // 4t+1

    public static void main(String[] args) {
        // 1) Parse parameters (default t=2, s=9)
        t = (args.length > 0) ? Integer.parseInt(args[0]) : 2;
        s = (args.length > 1) ? Integer.parseInt(args[1]) : 9;
        tracks = 2 * t + 1;
        n = s * tracks;
        minTrackHam = 4 * t + 1;

        System.out.println("Parameters: t=" + t + ", s=" + s +
                ", tracks=" + tracks + ", n=" + n);

        // 2) Build congruence classes P_i
        List<int[]> P = buildClasses(n, tracks);
        printClasses(P);

        // 3) Build Ci as all cyclic shifts of each P_i ordering
        List<List<int[]>> C = new ArrayList<>();
        for (int i = 0; i < tracks; i++) {
            C.add(cyclicShifts(P.get(i)));
        }
        // quick sanity: Hamming distance inside each Ci is s (>= 4t+1)
        sanityCheckTrackHamming(C);

        // 4) Interleave one codeword from each Ci (here: first choice)
        int[] codeword = interleave(Arrays.asList(
                C.get(0).get(0), C.get(1).get(0), C.get(2).get(0),
                C.get(3).get(0), C.get(4).get(0)
        ));
        System.out.println("Example interleaved codeword on [1.." + n + "]:");
        System.out.println(Arrays.toString(codeword));
        System.out.println("Is permutation of [1.." + n + "]? " + isPermutation(codeword, n));

        // 5) Example: build a second codeword using different shifts and compute Ulam distance
        int[] codeword2 = interleave(Arrays.asList(
                C.get(0).get(1), C.get(1).get(2), C.get(2).get(3),
                C.get(3).get(4), C.get(4).get(5)
        ));
        int du = ulamDistance(codeword, codeword2);
        System.out.println("Ulam distance between two codewords: " + du);

        // 6) Size of the constructed code C (cartesian product of shifts)
        long sizeC = 1;
        for (List<int[]> Ci : C) sizeC *= Ci.size(); // here s^tracks
        System.out.println("Code size |C| = product_i |C_i| = " + sizeC +
                " (expected " + s + "^" + tracks + ").");

        // Note: By Theorem 17, min Ulam distance of C is >= 2t+1
        System.out.println("Theorem 17 guarantee: d_U(C) >= " + (2 * t + 1));
    }

    // Build P_i = { i, i+(2t+1), i+2(2t+1), ... } for i=1..tracks
    static List<int[]> buildClasses(int n, int tracks) {
        List<int[]> res = new ArrayList<>();
        for (int i = 1; i <= tracks; i++) {
            int[] cls = new int[n / tracks];
            int val = i, k = 0;
            while (val <= n) { cls[k++] = val; val += tracks; }
            res.add(cls);
        }
        return res;
    }

    static void printClasses(List<int[]> P) {
        for (int i = 0; i < P.size(); i++) {
            System.out.println("P" + (i + 1) + " = " + Arrays.toString(P.get(i)));
        }
        System.out.println();
    }

    // All cyclic shifts of a base array
    static List<int[]> cyclicShifts(int[] base) {
        int s = base.length;
        List<int[]> shifts = new ArrayList<>(s);
        for (int sh = 0; sh < s; sh++) {
            int[] v = new int[s];
            for (int j = 0; j < s; j++) v[j] = base[(j + sh) % s];
            shifts.add(v);
        }
        return shifts;
    }

    // Check that within each track Ci, any two codewords have Hamming distance == s
    static void sanityCheckTrackHamming(List<List<int[]>> C) {
        for (int i = 0; i < C.size(); i++) {
            List<int[]> Ci = C.get(i);
            for (int a = 0; a < Ci.size(); a++) {
                for (int b = a + 1; b < Ci.size(); b++) {
                    int dh = hamming(Ci.get(a), Ci.get(b));
                    if (dh != s) {
                        throw new AssertionError("Track " + (i + 1) +
                                " pair has Hamming " + dh + " != " + s);
                    }
                }
            }
        }
        System.out.println("Each Ci has min Hamming = " + s + " (>= " + minTrackHam + ").");
    }

    // Interleave codewords w1,...,w_tracks (each length s) into permutation of [n]
    static int[] interleave(List<int[]> ws) {
        int tracks = ws.size(), s = ws.get(0).length;
        int[] out = new int[tracks * s];
        int idx = 0;
        for (int pos = 0; pos < s; pos++) {
            for (int t = 0; t < tracks; t++) out[idx++] = ws.get(t)[pos];
        }
        return out;
    }

    static int hamming(int[] a, int[] b) {
        int d = 0;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) d++;
        return d;
    }

    static boolean isPermutation(int[] arr, int n) {
        boolean[] seen = new boolean[n + 1];
        for (int v : arr) {
            if (v < 1 || v > n || seen[v]) return false;
            seen[v] = true;
        }
        return true;
    }

    // Ulam distance = n - LCS
    static int ulamDistance(int[] a, int[] b) {
        return a.length - lcsLength(a, b);
        // For large runs, you’d want LIS-on-mapped permutation; DP is fine for small n.
    }

    // Standard DP LCS (O(n^2))
    static int lcsLength(int[] a, int[] b) {
        int n = a.length, m = b.length;
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a[i - 1] == b[j - 1]) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp[n][m];
    }
}
