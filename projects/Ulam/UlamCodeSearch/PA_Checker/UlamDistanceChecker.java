import java.util.*;
import java.io.*;

public class UlamDistanceChecker {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java UlamDistanceChecker <filename>");
            return;
        }

        String filename = args[0];
        List<int[]> solution = readPermutationsFromFile(filename);

        if (solution.isEmpty()) {
            System.out.println("No permutations loaded from file.");
            return;
        }

        // n = length of a permutation (assume all have same length)
        int n = solution.get(0).length;
        int d = computeMinDistance(solution, n);

        System.out.println("n = " + n + ", d = " + d);
    }

    // Read permutations from a file (space-separated or bracketed)
    static List<int[]> readPermutationsFromFile(String filename) {
        List<int[]> perms = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Handle either "[1, 2, 3]" or "1 2 3"
                if (line.startsWith("[") && line.endsWith("]")) {
                    line = line.substring(1, line.length() - 1); // remove brackets
                    String[] parts = line.split(",");
                    int[] perm = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        perm[i] = Integer.parseInt(parts[i].trim());
                    }
                    perms.add(perm);
                } else {
                    String[] parts = line.split("\\s+");
                    int[] perm = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        perm[i] = Integer.parseInt(parts[i].trim());
                    }
                    perms.add(perm);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return perms;
    }

    // Compute minimum Ulam distance across all pairs
    static int computeMinDistance(List<int[]> solution, int n) {
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < solution.size(); i++) {
            for (int j = i + 1; j < solution.size(); j++) {
                int dist = ulamDistance(solution.get(i), solution.get(j), n);
                minDist = Math.min(minDist, dist);
            }
        }
        return (minDist == Integer.MAX_VALUE ? 0 : minDist);
    }

    // Compute Ulam distance using LCS dynamic programming
    static int ulamDistance(int[] a, int[] b, int n) {
        int[][] dp = new int[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i - 1] == b[j - 1])
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                else
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return n - dp[n][n];
    }
}
