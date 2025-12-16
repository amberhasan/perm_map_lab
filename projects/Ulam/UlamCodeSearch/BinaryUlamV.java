import java.util.*;
import java.io.*;

public class BinaryUlamV {

    // PARAMETERS (edit these)
    static int N = 14;   // string length
    static int W = 7;    // number of 1s
    static int D = 4;    // required Ulam distance

    static List<int[]> strings = new ArrayList<>();
    static boolean[][] ok; // ok[i][j] = (Ulam distance >= D)   

    public static void main(String[] args) {

        generateStrings();
        buildCompatibilityGraph();

        List<Integer> bestClique = new ArrayList<>();
        backtrackClique(new ArrayList<>(), 0, bestClique);

        System.out.println("===================================");
        System.out.println("V(" + N + ",2," + D + ") >= " + bestClique.size());
        System.out.println("Binary strings:");
        for (int idx : bestClique) {
            printString(strings.get(idx));
        }

        String filename = "V_" + N + "_2_" + D + ".txt";
        writeToFile(bestClique, filename);
        System.out.println("Saved strings to file: " + filename);
        System.out.println("===================================");
    }

    /* =========================================================
       STEP 1: Generate all binary strings of length N with W ones
       ========================================================= */
    static void generateStrings() {
        int[] s = new int[N];
        generateRec(s, 0, W);
        System.out.println("Generated " + strings.size() + " binary strings.");
    }

    static void generateRec(int[] s, int pos, int onesLeft) {
        if (pos == N) {
            if (onesLeft == 0) {
                strings.add(s.clone());
            }
            return;
        }
        if (onesLeft > N - pos) return;

        // place 0
        s[pos] = 0;
        generateRec(s, pos + 1, onesLeft);

        // place 1
        if (onesLeft > 0) {
            s[pos] = 1;
            generateRec(s, pos + 1, onesLeft - 1);
        }
    }

    /* =========================================================
       STEP 2: Build graph based on Ulam distance
       ========================================================= */
    static void buildCompatibilityGraph() {
        int m = strings.size();
        ok = new boolean[m][m];

        for (int i = 0; i < m; i++) {
            ok[i][i] = true;
            for (int j = i + 1; j < m; j++) {
                int dist = ulamDistance(strings.get(i), strings.get(j));
                ok[i][j] = ok[j][i] = (dist >= D);
            }
        }
    }

    /* =========================================================
       STEP 3: Maximum clique via backtracking (safe for n=12)
       ========================================================= */
    static void backtrackClique(List<Integer> current, int start, List<Integer> best) {

        // pruning
        if (current.size() + (strings.size() - start) <= best.size())
            return;

        if (current.size() > best.size()) {
            best.clear();
            best.addAll(current);
        }

        for (int i = start; i < strings.size(); i++) {
            boolean compatible = true;
            for (int j : current) {
                if (!ok[i][j]) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                current.add(i);
                backtrackClique(current, i + 1, best);
                current.remove(current.size() - 1);
            }
        }
    }

    /* =========================================================
       STEP 4: Ulam distance via LCS
       ========================================================= */
    static int ulamDistance(int[] a, int[] b) {
        int[][] dp = new int[N + 1][N + 1];

        for (int i = 1; i <= N; i++) {
            for (int j = 1; j <= N; j++) {
                if (a[i - 1] == b[j - 1])
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                else
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return N - dp[N][N];
    }

    /* =========================================================
       Utilities
       ========================================================= */
    static void printString(int[] s) {
        for (int x : s) System.out.print(x);
        System.out.println();
    }

    static void writeToFile(List<Integer> clique, String filename) {
        try (PrintWriter out = new PrintWriter(filename)) {
            for (int idx : clique) {
                int[] s = strings.get(idx);
                for (int x : s) out.print(x);
                out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
