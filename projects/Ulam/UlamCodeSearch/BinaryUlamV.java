import java.util.*;

public class BinaryUlamV {

    static int N;     // length
    static int W;     // number of 1s
    static int D;     // Ulam distance threshold
    static List<int[]> strings = new ArrayList<>();
    static boolean[][] ok; // ok[i][j] = distance >= D

    public static void main(String[] args) {
        N = 12;
        W = 6;
        D = 4;

        generateStrings();
        buildGraph();

        List<Integer> best = new ArrayList<>();
        backtrack(new ArrayList<>(), 0, best);

        System.out.println("V(" + N + ",2," + D + ") >= " + best.size());
        for (int i : best) {
            print(strings.get(i));
        }
    }

    // Generate all binary strings with exactly W ones
    static void generateStrings() {
        int[] s = new int[N];
        genRec(s, 0, W);
        System.out.println("Generated " + strings.size() + " strings");
    }

    static void genRec(int[] s, int pos, int onesLeft) {
        if (pos == N) {
            if (onesLeft == 0)
                strings.add(s.clone());
            return;
        }
        if (onesLeft > N - pos) return;

        s[pos] = 0;
        genRec(s, pos + 1, onesLeft);

        if (onesLeft > 0) {
            s[pos] = 1;
            genRec(s, pos + 1, onesLeft - 1);
        }
    }

    // Build compatibility graph
    static void buildGraph() {
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

    // Brute-force maximum clique (backtracking)
    static void backtrack(List<Integer> cur, int start, List<Integer> best) {
        if (cur.size() + (strings.size() - start) <= best.size())
            return;

        if (cur.size() > best.size()) {
            best.clear();
            best.addAll(cur);
        }

        for (int i = start; i < strings.size(); i++) {
            boolean good = true;
            for (int j : cur) {
                if (!ok[i][j]) {
                    good = false;
                    break;
                }
            }
            if (good) {
                cur.add(i);
                backtrack(cur, i + 1, best);
                cur.remove(cur.size() - 1);
            }
        }
    }

    // Ulam distance via LCS
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

    static void print(int[] s) {
        for (int x : s) System.out.print(x);
        System.out.println();
    }
}
