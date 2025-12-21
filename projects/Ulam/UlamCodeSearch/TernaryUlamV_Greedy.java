import java.util.*;
import java.io.*;

/**
 * Randomized greedy construction for V(N,3,D)
 * with constant composition over alphabet {0,1,2}.
 *
 * Exact Ulam distance:
 *   d_U(a,b) = N - LCS(a,b)
 *
 * LCS computed by DP in O(N^2).
 */
public class TernaryUlamV_Greedy {

    // =========================
    // PARAMETERS
    // =========================
    static int N = 9;          // length
    static int C1 = 3;          // number of 1s
    static int C2 = 3;          // number of 2s
    static int D  = 6;          // minimum Ulam distance
    static int ITER = 20000;
    static int VERIFY = 1;

    // Words
    static byte[][] words;

    // Compatibility graph
    static BitSet[] adj;

    public static void main(String[] args) {
        sanityCheck();

        // 1) generate all constant-composition ternary strings
        List<byte[]> list = new ArrayList<>();
        generateWords(list, new byte[N], 0, C1, C2);
        words = list.toArray(new byte[0][]);

        System.out.println("Generated " + words.length +
                " ternary strings (N=" + N +
                ", C1=" + C1 + ", C2=" + C2 + ").");

        // 2) build graph
        long t0 = System.currentTimeMillis();
        buildGraphExact();
        long t1 = System.currentTimeMillis();
        System.out.println("Built compatibility graph in " + (t1 - t0) + " ms.");

        // 3) randomized greedy
        Random rng = new Random(1);
        List<Integer> best = new ArrayList<>();

        for (int iter = 1; iter <= ITER; iter++) {
            List<Integer> clique = randomizedGreedy(rng);
            if (clique.size() > best.size()) {
                best = clique;
                System.out.println("New best size: " + best.size() +
                        " (iter " + iter + ")");
            }
        }

        // 4) verify
        if (VERIFY == 1) {
            int minDist = verifyClique(best);
            System.out.println("Verified min Ulam distance = " + minDist);
        }

        // 5) output
        System.out.println("===================================");
        System.out.println("V(" + N + ",3," + D + ") >= " + best.size());
        System.out.println("Ternary strings:");

        for (int v : best) {
            printWord(words[v]);
        }

        String filename = "V_" + N + "_3_" + D + "_greedy_exact.txt";
        writeToFile(best, filename);

        System.out.println("Saved to file: " + filename);
        System.out.println("===================================");
    }

    // =========================
    // RANDOMIZED GREEDY
    // =========================
    static List<Integer> randomizedGreedy(Random rng) {
        BitSet candidates = new BitSet(words.length);
        candidates.set(0, words.length);

        List<Integer> clique = new ArrayList<>();

        while (!candidates.isEmpty()) {
            int v = randomFromBitSet(candidates, rng);
            clique.add(v);
            candidates.and(adj[v]);
        }
        return clique;
    }

    static int randomFromBitSet(BitSet bs, Random rng) {
        int k = rng.nextInt(bs.cardinality());
        int v = bs.nextSetBit(0);
        for (int i = 0; i < k; i++) {
            v = bs.nextSetBit(v + 1);
        }
        return v;
    }

    // =========================
    // BUILD GRAPH (EXACT LCS)
    // =========================
    static void buildGraphExact() {
        int m = words.length;
        adj = new BitSet[m];
        for (int i = 0; i < m; i++) adj[i] = new BitSet(m);

        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                int dist = ulamDistanceExact(words[i], words[j]);
                if (dist >= D) {
                    adj[i].set(j);
                    adj[j].set(i);
                }
            }
        }
    }

    static int ulamDistanceExact(byte[] a, byte[] b) {
        int[] dp = new int[N + 1];

        for (int i = 1; i <= N; i++) {
            int prevDiag = 0;
            for (int j = 1; j <= N; j++) {
                int temp = dp[j];
                if (a[i - 1] == b[j - 1]) {
                    dp[j] = prevDiag + 1;
                } else {
                    dp[j] = Math.max(dp[j], dp[j - 1]);
                }
                prevDiag = temp;
            }
        }
        return N - dp[N];
    }

    // =========================
    // VERIFY
    // =========================
    static int verifyClique(List<Integer> clique) {
        if (clique.size() < 2) return N;
        int min = Integer.MAX_VALUE;

        for (int i = 0; i < clique.size(); i++) {
            for (int j = i + 1; j < clique.size(); j++) {
                int d = ulamDistanceExact(
                        words[clique.get(i)],
                        words[clique.get(j)]);
                min = Math.min(min, d);
            }
        }
        return min;
    }

    // =========================
    // GENERATE CONSTANT COMPOSITION WORDS
    // =========================
    static void generateWords(List<byte[]> out, byte[] cur,
                              int pos, int c1, int c2) {
        if (pos == N) {
            if (c1 == 0 && c2 == 0) {
                out.add(cur.clone());
            }
            return;
        }

        int remaining = N - pos;
        if (c1 + c2 > remaining) return;

        // 0
        cur[pos] = 0;
        generateWords(out, cur, pos + 1, c1, c2);

        // 1
        if (c1 > 0) {
            cur[pos] = 1;
            generateWords(out, cur, pos + 1, c1 - 1, c2);
        }

        // 2
        if (c2 > 0) {
            cur[pos] = 2;
            generateWords(out, cur, pos + 1, c1, c2 - 1);
        }
    }

    // =========================
    // OUTPUT
    // =========================
    static void printWord(byte[] w) {
        for (byte b : w) System.out.print(b);
        System.out.println();
    }

    static void writeToFile(List<Integer> clique, String filename) {
        try (PrintWriter out = new PrintWriter(filename)) {
            for (int v : clique) {
                for (byte b : words[v]) out.print(b);
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
        if (N <= 0) throw new IllegalArgumentException("N must be positive.");
        if (C1 < 0 || C2 < 0 || C1 + C2 > N)
            throw new IllegalArgumentException("Invalid composition.");
        if (D < 0 || D > N) throw new IllegalArgumentException("Invalid D.");
        if (ITER <= 0) throw new IllegalArgumentException("ITER must be positive.");
    }
}
