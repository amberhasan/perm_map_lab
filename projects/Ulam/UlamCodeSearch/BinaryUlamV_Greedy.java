import java.util.*;
import java.io.*;

/**
 * Randomized greedy construction for V(N,2,D) with constant weight W.
 *
 * Correctness: Uses exact Ulam distance for binary strings:
 *   d_U(a,b) = N - LCS(a,b)
 * where LCS is computed by DP (O(N^2) per pair).
 *
 * Purpose: Fast constructive lower bounds (not exact maximum).
 */
public class BinaryUlamV_Greedy {

    // =========================
    // PARAMETERS
    // =========================
    static int N = 12;          // length
    static int W = 2;           // number of 1s
    static int D = 10;           // minimum Ulam distance
    static int ITER = 20000;    // random restarts
    static int VERIFY = 1;      // set to 1 to verify final clique

    // Strings as bitmasks
    static long[] masks;

    // Compatibility graph: adj[v] contains neighbors u with dist(v,u) >= D
    static BitSet[] adj;

    public static void main(String[] args) {
        sanityCheck();

        // 1) generate all weight-W strings
        List<Long> list = new ArrayList<>();
        generateMasks(list, 0, W, 0L);
        masks = new long[list.size()];
        for (int i = 0; i < list.size(); i++) masks[i] = list.get(i);

        System.out.println("Generated " + masks.length +
                " binary strings (N=" + N + ", W=" + W + ").");

        // 2) build graph (exact DP distance)
        long t0 = System.currentTimeMillis();
        buildGraphExact();
        long t1 = System.currentTimeMillis();
        System.out.println("Built compatibility graph in " + (t1 - t0) + " ms.");

        // 3) randomized greedy
        Random rng = new Random(1); // reproducible
        List<Integer> best = new ArrayList<>();

        for (int iter = 1; iter <= ITER; iter++) {
            List<Integer> clique = randomizedGreedy(rng);

            if (clique.size() > best.size()) {
                best = clique;
                System.out.println("New best size: " + best.size() + " (iter " + iter + ")");
            }
        }

        // 4) verify best clique (optional but recommended)
        if (VERIFY == 1) {
            int minDist = verifyClique(best);
            System.out.println("Verified min pairwise Ulam distance in best set = " + minDist);
            if (minDist < D) {
                System.out.println("WARNING: clique verification failed (minDist < D). Something is wrong.");
            }
        }

        // 5) output
        System.out.println("===================================");
        System.out.println("Greedy lower bound:");
        System.out.println("V(" + N + ",2," + D + ") >= " + best.size());
        System.out.println("Binary strings:");

        for (int v : best) {
            printMask(masks[v]);
        }

        String filename = "V_" + N + "_2_" + D + "_greedy_exact.txt";
        writeToFile(best, filename);

        System.out.println("Saved to file: " + filename);
        System.out.println("===================================");
    }

    // =========================
    // RANDOMIZED GREEDY CLIQUE CONSTRUCTION
    // =========================
    static List<Integer> randomizedGreedy(Random rng) {
        BitSet candidates = new BitSet(masks.length);
        candidates.set(0, masks.length);

        List<Integer> clique = new ArrayList<>();

        while (!candidates.isEmpty()) {
            int v = randomFromBitSet(candidates, rng);
            clique.add(v);
            candidates.and(adj[v]); // keep only vertices compatible with all chosen so far
        }
        return clique;
    }

    static int randomFromBitSet(BitSet bs, Random rng) {
        int size = bs.cardinality();
        int k = rng.nextInt(size);

        int v = bs.nextSetBit(0);
        for (int i = 0; i < k; i++) {
            v = bs.nextSetBit(v + 1);
        }
        return v;
    }

    // =========================
    // BUILD GRAPH USING EXACT ULAM DISTANCE (DP LCS)
    // =========================
    static void buildGraphExact() {
        int m = masks.length;
        adj = new BitSet[m];
        for (int i = 0; i < m; i++) adj[i] = new BitSet(m);

        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                int dist = ulamDistanceBinaryExact(masks[i], masks[j]);
                if (dist >= D) {
                    adj[i].set(j);
                    adj[j].set(i);
                }
            }
        }
    }

    // =========================
    // EXACT ULAM DISTANCE FOR BINARY STRINGS:
    // d_U(a,b) = N - LCS(a,b)
    //
    // LCS DP is O(N^2), but N is small (e.g., 14, 16, 18).
    // =========================
    static int ulamDistanceBinaryExact(long a, long b) {
        // dp[j] = dp for current row; prevDiag stores dp[i-1][j-1]
        int[] dp = new int[N + 1];

        for (int i = 1; i <= N; i++) {
            int prevDiag = 0;
            int abit = (int) ((a >>> (i - 1)) & 1L);

            for (int j = 1; j <= N; j++) {
                int temp = dp[j]; // dp[i-1][j]
                int bbit = (int) ((b >>> (j - 1)) & 1L);

                if (abit == bbit) {
                    dp[j] = prevDiag + 1;
                } else {
                    dp[j] = Math.max(dp[j], dp[j - 1]);
                }
                prevDiag = temp;
            }
        }
        int lcs = dp[N];
        return N - lcs;
    }

    // =========================
    // VERIFY FINAL SET (MIN PAIRWISE DISTANCE)
    // =========================
    static int verifyClique(List<Integer> clique) {
        int minDist = Integer.MAX_VALUE;

        for (int ii = 0; ii < clique.size(); ii++) {
            for (int jj = ii + 1; jj < clique.size(); jj++) {
                long a = masks[clique.get(ii)];
                long b = masks[clique.get(jj)];
                int d = ulamDistanceBinaryExact(a, b);
                if (d < minDist) minDist = d;
            }
        }
        if (clique.size() < 2) return N; // vacuously large
        return minDist;
    }

    // =========================
    // GENERATE ALL WEIGHT-W MASKS
    // =========================
    static void generateMasks(List<Long> out, int pos, int onesLeft, long mask) {
        if (pos == N) {
            if (onesLeft == 0) out.add(mask);
            return;
        }
        if (onesLeft > N - pos) return;

        // put 0
        generateMasks(out, pos + 1, onesLeft, mask);

        // put 1
        if (onesLeft > 0) {
            generateMasks(out, pos + 1, onesLeft - 1, mask | (1L << pos));
        }
    }

    // =========================
    // OUTPUT
    // =========================
    static void printMask(long mask) {
        for (int i = 0; i < N; i++) {
            System.out.print(((mask >>> i) & 1L) == 1L ? '1' : '0');
        }
        System.out.println();
    }

    static void writeToFile(List<Integer> clique, String filename) {
        try (PrintWriter out = new PrintWriter(filename)) {
            for (int v : clique) {
                long mask = masks[v];
                for (int i = 0; i < N; i++) {
                    out.print(((mask >>> i) & 1L) == 1L ? '1' : '0');
                }
                out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // SANITY CHECK
    // =========================
    static void sanityCheck() {
        if (N <= 0 || N > 63) throw new IllegalArgumentException("Require 1 <= N <= 63.");
        if (W < 0 || W > N) throw new IllegalArgumentException("Require 0 <= W <= N.");
        if (D < 0 || D > N) throw new IllegalArgumentException("Require 0 <= D <= N.");
        if (ITER <= 0) throw new IllegalArgumentException("ITER must be positive.");
    }
}
