import java.util.*;
import java.io.*;

/**
 * Randomized greedy construction for V(N,2,D).
 *
 * - Binary strings with constant weight W
 * - Correct Ulam distance for binary strings
 * - Randomized greedy clique construction
 *
 * This is intended for fast constructive lower bounds,
 * not exact maximum clique.
 */
public class BinaryUlamV_Greedy {

    // =========================
    // PARAMETERS
    // =========================
    static int N = 14;     // string length
    static int W = 7;      // number of 1s
    static int D = 4;      // minimum Ulam distance
    static int ITER = 20000; // number of random restarts

    // binary strings as bitmasks
    static long[] masks;

    // adjacency graph
    static BitSet[] adj;

    // =========================
    // MAIN
    // =========================
    public static void main(String[] args) {
        sanityCheck();

        // 1) generate all constant-weight binary strings
        List<Long> list = new ArrayList<>();
        generateMasks(list, 0, W, 0L);
        masks = new long[list.size()];
        for (int i = 0; i < list.size(); i++) masks[i] = list.get(i);

        System.out.println("Generated " + masks.length +
                " binary strings (N=" + N + ", W=" + W + ").");

        // 2) build compatibility graph
        buildGraph();
        System.out.println("Built compatibility graph.");

        // 3) randomized greedy search
        Random rng = new Random(1); // fixed seed = reproducible
        List<Integer> best = new ArrayList<>();

        for (int iter = 1; iter <= ITER; iter++) {
            List<Integer> clique = randomizedGreedy(rng);

            if (clique.size() > best.size()) {
                best = clique;
                System.out.println("New best size: " + best.size()
                        + " (iteration " + iter + ")");
            }
        }

        // 4) output
        System.out.println("===================================");
        System.out.println("Greedy lower bound:");
        System.out.println("V(" + N + ",2," + D + ") >= " + best.size());
        System.out.println("Binary strings:");

        for (int v : best) {
            printMask(masks[v]);
        }

        String filename = "V_" + N + "_2_" + D + "_greedy.txt";
        writeToFile(best, filename);
        System.out.println("Saved to file: " + filename);
        System.out.println("===================================");
    }

    // =========================
    // RANDOMIZED GREEDY
    // =========================
    static List<Integer> randomizedGreedy(Random rng) {
        BitSet candidates = new BitSet(masks.length);
        candidates.set(0, masks.length);

        List<Integer> clique = new ArrayList<>();

        while (!candidates.isEmpty()) {
            int v = randomFromBitSet(candidates, rng);
            clique.add(v);
            candidates.and(adj[v]); // keep only compatible vertices
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
    // GRAPH CONSTRUCTION
    // =========================
    static void buildGraph() {
        int m = masks.length;
        adj = new BitSet[m];
        for (int i = 0; i < m; i++) adj[i] = new BitSet(m);

        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                if (ulamDistanceBinary(masks[i], masks[j]) >= D) {
                    adj[i].set(j);
                    adj[j].set(i);
                }
            }
        }
    }

    // =========================
    // ULAM DISTANCE (CORRECT)
    //
    // For binary strings:
    // diff(k) = #1_a(0..k) - #1_b(0..k)
    // M = max |diff(k)|
    // d_U = 2M
    // =========================
    static int ulamDistanceBinary(long a, long b) {
        int diff = 0;
        int maxAbs = 0;

        for (int i = 0; i < N; i++) {
            int abit = (int) ((a >>> i) & 1L);
            int bbit = (int) ((b >>> i) & 1L);
            diff += abit - bbit;
            maxAbs = Math.max(maxAbs, Math.abs(diff));
        }
        return 2 * maxAbs;
    }

    // =========================
    // GENERATION
    // =========================
    static void generateMasks(List<Long> out, int pos, int onesLeft, long mask) {
        if (pos == N) {
            if (onesLeft == 0) out.add(mask);
            return;
        }
        if (onesLeft > N - pos) return;

        generateMasks(out, pos + 1, onesLeft, mask);
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

    static void sanityCheck() {
        if (N <= 0 || N > 63)
            throw new IllegalArgumentException("Require 1 ≤ N ≤ 63.");
        if (W < 0 || W > N)
            throw new IllegalArgumentException("Require 0 ≤ W ≤ N.");
        if (D < 0 || D > N)
            throw new IllegalArgumentException("Require 0 ≤ D ≤ N.");
    }
}
