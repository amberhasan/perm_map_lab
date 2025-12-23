import java.util.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Multistage "block shuffle" construction inspired by:
 * "Explicit Good Codes Approaching Distance 1 in Ulam Metric"
 *
 * Experimental PA builder for U(n,d) using structured shuffle-lift sampling.
 *
 * Example runs:
 *   java UlamShuffleLift q=3 n=243 d=180 samples=20000
 *   java UlamShuffleLift n=81 d=60
 */
public class UlamShuffleLift {

    // =====================================================
    // Construction
    // =====================================================

    public static int[] build(int q, int ell, int[][] D, int[][] shufflers) {
        int n = ipow(q, ell);

        if (shufflers.length != ell)
            throw new IllegalArgumentException("Need exactly ell shufflers.");

        int[] pi = new int[n];
        for (int i = 0; i < n; i++) pi[i] = i;

        // Precompute base-q digits for each position
        int[][] digits = new int[n][ell];
        for (int pos = 0; pos < n; pos++) {
            int x = pos;
            for (int d = ell - 1; d >= 0; d--) {
                digits[pos][d] = x % q;
                x /= q;
            }
        }

        for (int stage = 0; stage < ell; stage++) {
            int blocks = n / q;
            if (shufflers[stage].length != blocks)
                throw new IllegalArgumentException("Stage " + stage + " shuffler must have length " + blocks);

            int[] next = new int[n];

            for (int pos = 0; pos < n; pos++) {
                int blockIndex = blockIndexExcludingDigit(digits[pos], q, stage);
                int c = shufflers[stage][blockIndex];
                int x = digits[pos][stage];
                int y = D[c][x];
                int srcPos = replaceDigitAndPack(digits[pos], q, stage, y);
                next[pos] = pi[srcPos];
            }
            pi = next;
        }
        return pi;
    }

    private static int blockIndexExcludingDigit(int[] dig, int q, int excludedDigit) {
        int idx = 0;
        for (int i = 0; i < dig.length; i++) {
            if (i != excludedDigit) idx = idx * q + dig[i];
        }
        return idx;
    }

    private static int replaceDigitAndPack(int[] dig, int q, int posDigit, int newVal) {
        int idx = 0;
        for (int i = 0; i < dig.length; i++) {
            idx = idx * q + (i == posDigit ? newVal : dig[i]);
        }
        return idx;
    }

    private static int ipow(int a, int e) {
        int r = 1;
        for (int i = 0; i < e; i++) r *= a;
        return r;
    }

    // =====================================================
    // Ulam Distance
    // =====================================================

    public static int ulamDistance(int[] pi, int[] pj) {
        int n = pi.length;
        int[] inv = new int[n];
        for (int i = 0; i < n; i++) inv[pi[i]] = i;

        int[] seq = new int[n];
        for (int i = 0; i < n; i++) seq[i] = inv[pj[i]];

        return n - lisLength(seq);
    }

    private static int lisLength(int[] a) {
        int[] tails = new int[a.length];
        int size = 0;
        for (int x : a) {
            int i = Arrays.binarySearch(tails, 0, size, x);
            if (i < 0) i = -i - 1;
            tails[i] = x;
            if (i == size) size++;
        }
        return size;
    }

    // =====================================================
    // Build PA via sampling
    // =====================================================

    public static List<int[]> buildUlamPA(int q, int ell, int[][] D, int samples, int minDistance) {
        Random rng = new Random();
        int n = ipow(q, ell);
        int blocks = n / q;
        int p = D.length;

        List<int[]> PA = new ArrayList<>();

        for (int t = 0; t < samples; t++) {
            int[][] shufflers = new int[ell][blocks];
            for (int i = 0; i < ell; i++)
                for (int j = 0; j < blocks; j++)
                    shufflers[i][j] = rng.nextInt(p);

            int[] pi = build(q, ell, D, shufflers);

            boolean ok = true;
            for (int[] pj : PA) {
                if (ulamDistance(pi, pj) < minDistance) {
                    ok = false;
                    break;
                }
            }
            if (ok) PA.add(pi);
        }
        return PA;
    }

    // =====================================================
    // Argument Parsing Helpers
    // =====================================================

    private static Map<String, Integer> parseArgs(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        for (String arg : args) {
            if (!arg.contains("=")) continue;
            String[] parts = arg.split("=");
            if (parts.length != 2) continue;
            map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        }
        return map;
    }

    private static int computeEllIfPower(int n, int q) {
        int ell = 0;
        int cur = 1;
        while (cur < n) {
            cur *= q;
            ell++;
        }
        if (cur != n)
            throw new IllegalArgumentException("n must be a power of q. Got n=" + n + ", q=" + q);
        return ell;
    }

    // =====================================================
    // Main
    // =====================================================

    public static void main(String[] args) {

        // ----- Defaults -----
        int q = 3;
        int n = 81;
        int d = 60;
        int samples = 50_000;

        // ----- Parse key=value args -----
        try {
            Map<String, Integer> params = parseArgs(args);
            if (params.containsKey("q")) q = params.get("q");
            if (params.containsKey("n")) n = params.get("n");
            if (params.containsKey("d")) d = params.get("d");
            if (params.containsKey("samples")) samples = params.get("samples");
        } catch (Exception e) {
            System.err.println("Usage: java UlamShuffleLift q=<q> n=<n> d=<d> samples=<samples>");
            return;
        }

        int ell;
        try {
            ell = computeEllIfPower(n, q);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        // ----- Ground permutations D ⊆ S_q -----
        int[][] D = {
                {0,1,2},
                {2,1,0},
                {1,0,2},
                {1,2,0}
        };

        System.out.println("====================================");
        System.out.println("UlamShuffleLift experiment");
        System.out.println("q = " + q);
        System.out.println("n = " + n);
        System.out.println("ell = " + ell);
        System.out.println("d = " + d);
        System.out.println("samples = " + samples);
        System.out.println("====================================");

        List<int[]> PA = buildUlamPA(q, ell, D, samples, d);

        System.out.println("PA size = " + PA.size());

        // ----- Write results -----
        String filename = "ulam_results_q" + q + "_n" + n + "_d" + d + ".txt";
        try (FileWriter out = new FileWriter(filename, true)) {
            out.write("====================================\n");
            out.write("Timestamp: " + LocalDateTime.now() + "\n");
            out.write("q = " + q + "\n");
            out.write("n = " + n + "\n");
            out.write("ell = " + ell + "\n");
            out.write("d = " + d + "\n");
            out.write("samples = " + samples + "\n");
            out.write("PA size = " + PA.size() + "\n\n");
        } catch (IOException e) {
            System.err.println("Error writing results to file");
            e.printStackTrace();
        }

        System.out.println("Results written to " + filename);
    }
}
