import java.util.*;
import java.io.*;

/**
 * Optimized search for V(N,2,D) using:
 * - bitmask representation
 * - correct O(N) Ulam distance for binary strings
 * - BitSet adjacency graph
 * - Bron–Kerbosch with pivoting
 */
public class BinaryUlamV_Optimized {

    // =========================
    // PARAMETERS
    // =========================
    static int N = 14;   // length
    static int W = 7;    // number of 1s
    static int D = 4;    // minimum Ulam distance

    // binary strings as bitmasks
    static long[] masks;

    // adjacency graph
    static BitSet[] adj;

    // best clique found
    static BitSet bestClique = new BitSet();
    static int bestSize = 0;

    // =========================
    // MAIN
    // =========================
    public static void main(String[] args) {
        sanityCheck();

        // 1) generate all constant-weight masks
        List<Long> list = new ArrayList<>();
        generateMasks(list, 0, W, 0L);
        masks = new long[list.size()];
        for (int i = 0; i < list.size(); i++) masks[i] = list.get(i);

        System.out.println("Generated " + masks.length +
                " binary strings (N=" + N + ", W=" + W + ").");

        // 2) build compatibility graph
        buildGraph();
        System.out.println("Built graph with " + masks.length + " vertices.");

        // 3) maximum clique
        BitSet R = new BitSet();
        BitSet P = new BitSet(masks.length);
        P.set(0, masks.length);
        BitSet X = new BitSet();

        bronKerboschPivot(R, P, X);

        // 4) output
        System.out.println("===================================");
        System.out.println("V(" + N + ",2," + D + ") >= " + bestSize);
        System.out.println("Binary strings:");

        List<Integer> clique = bitsetToList(bestClique);
        for (int v : clique) {
            printMask(masks[v]);
        }

        String filename = "V_" + N + "_2_" + D + ".txt";
        writeToFile(clique, filename);

        System.out.println("Saved strings to file: " + filename);
        System.out.println("===================================");
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

        // 0
        generateMasks(out, pos + 1, onesLeft, mask);

        // 1
        if (onesLeft > 0) {
            generateMasks(out, pos + 1, onesLeft - 1, mask | (1L << pos));
        }
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
    // CORRECT ULAM DISTANCE
    //
    // For binary strings:
    // diff(k) = #1_a(0..k) - #1_b(0..k)
    // M = max |diff(k)|
    // LCS = N - 2M
    // d_U = 2M
    // =========================
    static int ulamDistanceBinary(long a, long b) {
        int diff = 0;
        int maxAbs = 0;

        for (int i = 0; i < N; i++) {
            int abit = (int) ((a >>> i) & 1L);
            int bbit = (int) ((b >>> i) & 1L);
            diff += abit - bbit;
            int ad = Math.abs(diff);
            if (ad > maxAbs) maxAbs = ad;
        }
        return 2 * maxAbs;
    }

    // =========================
    // BRON–KERBOSCH WITH PIVOT
    // =========================
    static void bronKerboschPivot(BitSet R, BitSet P, BitSet X) {
        if (R.cardinality() + P.cardinality() <= bestSize) return;

        if (P.isEmpty() && X.isEmpty()) {
            int size = R.cardinality();
            if (size > bestSize) {
                bestSize = size;
                bestClique = (BitSet) R.clone();
            }
            return;
        }

        int u = choosePivot(P, X);
        BitSet candidates = (BitSet) P.clone();
        if (u != -1) candidates.andNot(adj[u]);

        for (int v = candidates.nextSetBit(0); v >= 0; v = candidates.nextSetBit(v + 1)) {
            BitSet R2 = (BitSet) R.clone();
            R2.set(v);

            BitSet P2 = (BitSet) P.clone();
            P2.and(adj[v]);

            BitSet X2 = (BitSet) X.clone();
            X2.and(adj[v]);

            bronKerboschPivot(R2, P2, X2);

            P.clear(v);
            X.set(v);

            if (R.cardinality() + P.cardinality() <= bestSize) return;
        }
    }

    static int choosePivot(BitSet P, BitSet X) {
        BitSet PX = (BitSet) P.clone();
        PX.or(X);
        if (PX.isEmpty()) return -1;

        int bestU = -1;
        int bestCount = -1;

        for (int u = PX.nextSetBit(0); u >= 0; u = PX.nextSetBit(u + 1)) {
            BitSet tmp = (BitSet) P.clone();
            tmp.and(adj[u]);
            int c = tmp.cardinality();
            if (c > bestCount) {
                bestCount = c;
                bestU = u;
            }
        }
        return bestU;
    }

    // =========================
    // OUTPUT UTILITIES
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

    static List<Integer> bitsetToList(BitSet bs) {
        List<Integer> list = new ArrayList<>();
        for (int v = bs.nextSetBit(0); v >= 0; v = bs.nextSetBit(v + 1)) {
            list.add(v);
        }
        return list;
    }

    static void sanityCheck() {
        if (N <= 0 || N > 63)
            throw new IllegalArgumentException("Require 1 <= N <= 63.");
        if (W < 0 || W > N)
            throw new IllegalArgumentException("Require 0 <= W <= N.");
        if (D < 0 || D > N)
            throw new IllegalArgumentException("Require 0 <= D <= N.");
    }
}
