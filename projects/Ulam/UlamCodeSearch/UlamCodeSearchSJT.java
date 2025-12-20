import java.util.*;

public class UlamCodeSearchSJT {

    static int n;
    static int d;

    public static void main(String[] args) {
        n = Integer.parseInt(args[0]);
        d = Integer.parseInt(args[1]);

        List<int[]> code = new ArrayList<>();
        SJTGenerator gen = new SJTGenerator(n);

        int[] p;
        while ((p = gen.next()) != null) {
            boolean ok = true;
            for (int[] q : code) {
                if (ulamDistance(p, q) < d) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                code.add(p);
            }
        }

        System.out.println("\nFinal U(" + n + "," + d + ") size = " + code.size());
        saveCodeToFile(code, n, d);
    }

    static int ulamDistance(int[] a, int[] b) {
        int n = a.length;
        int[][] dp = new int[n+1][n+1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i-1] == b[j-1])
                    dp[i][j] = dp[i-1][j-1] + 1;
                else
                    dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
            }
        }
        return n - dp[n][n];
    }

    static void saveCodeToFile(List<int[]> code, int n, int d) {
        String filename = "U" + n + "_" + d + "_SJT.txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("# U(" + n + "," + d + ") generated via SJT greedy");
            out.println("# size = " + code.size());
            for (int[] p : code) {
                for (int i = 0; i < p.length; i++) {
                    out.print(p[i]);
                    if (i + 1 < p.length) out.print(" ");
                }
                out.println();
            }
            System.out.println("Saved code to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }

}

class SJTGenerator {
    int n;
    int[] perm;
    int[] dir;
    boolean first = true;

    public SJTGenerator(int n) {
        this.n = n;
        perm = new int[n];
        dir = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i + 1;
            dir[i] = -1;
        }
    }

    public int[] next() {
        if (first) {
            first = false;
            return perm.clone();
        }

        int k = -1, kIdx = -1;

        for (int i = 0; i < n; i++) {
            int j = i + dir[i];
            if (j >= 0 && j < n && perm[i] > perm[j]) {
                if (k == -1 || perm[i] > k) {
                    k = perm[i];
                    kIdx = i;
                }
            }
        }

        if (k == -1) return null;

        int swapIdx = kIdx + dir[kIdx];
        swap(perm, kIdx, swapIdx);
        swap(dir, kIdx, swapIdx);

        for (int i = 0; i < n; i++) {
            if (perm[i] > k) dir[i] *= -1;
        }

        return perm.clone();
    }

    private void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
