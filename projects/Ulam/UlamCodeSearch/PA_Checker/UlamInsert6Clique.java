import java.io.*;
import java.util.*;

/**
 * Build a permutation array of size 120 on {1..6} by inserting symbol 6
 * into permutations of {1..5}, such that EVERY pair has Ulam distance = 2.
 *
 * This version backtracks over:
 *   (1) which base permutation is chosen next
 *   (2) where the 6 is inserted
 *
 * This is the correct search space.
 */
public class UlamInsert6Clique {

    static List<int[]> basePerms;
    static boolean[] used;
    static List<int[]> solution;
    static int maxSizeReached = 0;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java UlamInsert6Clique <input5perms.txt> <output6perms.txt>");
            return;
        }

        basePerms = readPermsLength5(args[0]);
        if (basePerms.size() != 120) {
            throw new RuntimeException("Expected 120 permutations, got " + basePerms.size());
        }

        used = new boolean[basePerms.size()];
        solution = new ArrayList<>();

        long start = System.currentTimeMillis();
        boolean ok = dfs();
        long end = System.currentTimeMillis();

        if (!ok) {
            System.out.println("❌ No solution found.");
            return;
        }

        writePerms(args[1], solution);
        System.out.println("✅ SUCCESS: constructed 120 permutations.");
        System.out.println("Time: " + (end - start) + " ms");
    }

    // ================= DFS BACKTRACKING =================

    static boolean dfs() {
        int size = solution.size();

        if (size > maxSizeReached) {
            maxSizeReached = size;
            System.out.println("New max size reached: " + maxSizeReached);
        }

        for (int i = 0; i < basePerms.size(); i++) {
            if (used[i]) continue;

            int[] base = basePerms.get(i);

            // Try all 6 insertion positions for symbol 6
            for (int pos = 0; pos <= 5; pos++) {
                int[] candidate = insert6(base, pos);

                if (!validAgainstAll(candidate)) continue;

                used[i] = true;
                solution.add(candidate);

                if (dfs()) return true;

                // backtrack
                solution.remove(solution.size() - 1);
                used[i] = false;
            }
        }

        return false;
    }

    static boolean validAgainstAll(int[] candidate) {
        for (int[] prev : solution) {
            if (ulamDistance(candidate, prev) != 2) return false;
        }
        return true;
    }

    // ================= ULAM DISTANCE =================

    /**
     * Ulam distance = n - LIS(inv(p) ∘ q)
     */
    static int ulamDistance(int[] p, int[] q) {
        int n = p.length;
        int[] inv = new int[n + 1];

        for (int i = 0; i < n; i++) {
            inv[p[i]] = i;
        }

        int[] seq = new int[n];
        for (int i = 0; i < n; i++) {
            seq[i] = inv[q[i]];
        }

        return n - lisLength(seq);
    }

    static int lisLength(int[] a) {
        int[] tails = new int[a.length];
        int size = 0;

        for (int x : a) {
            int lo = 0, hi = size;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (tails[mid] < x) lo = mid + 1;
                else hi = mid;
            }
            tails[lo] = x;
            if (lo == size) size++;
        }
        return size;
    }

    // ================= INSERTION =================

    static int[] insert6(int[] base5, int pos) {
        int[] out = new int[6];
        int j = 0;
        for (int i = 0; i < 6; i++) {
            if (i == pos) out[i] = 6;
            else out[i] = base5[j++];
        }
        return out;
    }

    // ================= FILE I/O =================

    static List<int[]> readPermsLength5(String filename) throws IOException {
        List<int[]> perms = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                if (parts.length != 5) {
                    throw new IOException("Invalid line: " + line);
                }

                int[] p = new int[5];
                for (int i = 0; i < 5; i++) {
                    p[i] = Integer.parseInt(parts[i]);
                }
                perms.add(p);
            }
        }
        return perms;
    }

    static void writePerms(String filename, List<int[]> perms) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (int[] p : perms) {
                for (int i = 0; i < p.length; i++) {
                    if (i > 0) pw.print(" ");
                    pw.print(p[i]);
                }
                pw.println();
            }
        }
    }
}
