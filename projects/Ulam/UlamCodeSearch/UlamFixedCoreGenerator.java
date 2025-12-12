import java.util.*;
import java.io.*;

public class UlamFixedCoreGenerator {
    static int n, d, k; // parameters
    static List<int[]> code = new ArrayList<>();
    static Random rand = new Random();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java UlamFixedCoreGenerator <n> <d>");
            System.exit(1);
        }

        n = Integer.parseInt(args[0]);
        d = Integer.parseInt(args[1]);
        k = n - d; // max allowed LCS

        long start = System.currentTimeMillis();
        buildCode();
        long end = System.currentTimeMillis();

        // Print results
        System.out.println("Constructed PA(" + n + "," + d + ") of size " + code.size());
        System.out.println("Elapsed time: " + (end - start) / 1000.0 + " seconds");

        // Save to file
        saveResults();
    }

    static void buildCode() {
        for (int shift = 0; shift < n; shift++) {
            int[] core = new int[k];
            int idx = 0;
            for (int i = 0; i < n && idx < k; i++) {
                core[idx++] = ((i + shift) % n) + 1; // rotated subsequence
            }

            // Tail = everything not in core
            List<Integer> tail = new ArrayList<>();
            Set<Integer> coreSet = new HashSet<>();
            for (int c : core) coreSet.add(c);
            for (int i = 1; i <= n; i++) {
                if (!coreSet.contains(i)) tail.add(i);
            }

            // Try multiple shuffles of tail
            int trials = 200;
            for (int t = 0; t < trials; t++) {
                Collections.shuffle(tail, rand);
                int[] perm = new int[n];
                for (int i = 0; i < k; i++) perm[i] = core[i];
                for (int i = 0; i < d; i++) perm[k + i] = tail.get(i);

                if (isValid(perm)) code.add(perm.clone());
            }
        }
    }

    static boolean isValid(int[] perm) {
        for (int[] existing : code) {
            int lcs = lcsLength(existing, perm);
            if (lcs > n - d) return false; // violates distance
        }
        return true;
    }

    static int lcsLength(int[] a, int[] b) {
        int[][] dp = new int[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i - 1] == b[j - 1])
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                else
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return dp[n][n];
    }

    static void saveResults() {
        // Create a unique filename for each run
        String filename = String.format("fixed_core_n%d_d%d.txt", n, d);

        try (FileWriter fw = new FileWriter(filename);  // overwrite mode (new file each run)
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)) {

            // Permutations
            for (int[] perm : code) {
                out.println(Arrays.toString(perm));
            }

            System.out.println("Results saved to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

}
