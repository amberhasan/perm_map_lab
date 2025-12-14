import java.io.*;
import java.util.*;

/**
 * Build a large Ulam-distance-2 code in S_7 by taking 720 permutations of length 6
 * (in a fixed input order) and inserting the symbol 7 into each one.
 *
 * Constraint: For the chosen length-7 permutations, every pair must have Ulam distance >= 2.
 *
 * Ulam distance for permutations p,q of length n:
 *   d_U(p,q) = n - LCS(p,q)
 * For permutations, LCS(p,q) = LIS( posInQ[p[0]], posInQ[p[1]], ... )
 */
public class Ulam7From6Backtrack {

    // Input permutations of length 6 (size should be 720)
    static List<int[]> base6 = new ArrayList<>();

    // For each i, there are 7 candidates: base6[i] with 7 inserted at position k
    static int[][][] cand7; // [i][k][7]

    // Best solution found
    static List<int[]> best = new ArrayList<>();
    static int bestSize = 0;

    // Current partial solution
    static List<int[]> current = new ArrayList<>();

    // Track which insertion position we chose for each i in current (for debugging)
    static int[] chosenPos; // chosenPos[i] = k used when we accepted i-th base perm, else -1

    // Cache inverse position arrays for current solution perms
    // invPositions.get(t)[value] = position of value in current.get(t)
    static List<int[]> invPositions = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Ulam7From6Backtrack <perms6_file> [output_file]");
            System.out.println("Example: java Ulam7From6Backtrack perms6.txt out_U7_d2.txt");
            return;
        }
        String inFile = args[0];
        String outFile = (args.length >= 2) ? args[1] : "U7_d2_solution.txt";

        readPerms6(inFile);

        if (base6.size() == 0) {
            System.out.println("No permutations loaded. Check file format.");
            return;
        }
        if (base6.get(0).length != 6) {
            System.out.println("Input permutations must be length 6.");
            return;
        }

        System.out.println("Loaded " + base6.size() + " permutations of length 6.");

        buildCandidates();

        chosenPos = new int[base6.size()];
        Arrays.fill(chosenPos, -1);

        // Backtracking search in the fixed order 0..(m-1)
        try {
            dfs(0);
        } catch (RuntimeException e) {
            if (!"DONE".equals(e.getMessage())) throw e;
        }

        System.out.println("\nBEST SIZE FOUND = " + bestSize);
        writeSolution(outFile, best);
        System.out.println("Wrote best solution to: " + outFile);
    }

    // -------------------- IO --------------------

    static void readPerms6(String filename) throws IOException {
        base6.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Allow formats like: "1 2 3 4 5 6" or "[1, 2, 3, 4, 5, 6]"
                line = line.replace("[", " ").replace("]", " ").replace(",", " ");
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 6) continue;

                int[] p = new int[6];
                for (int i = 0; i < 6; i++) {
                    p[i] = Integer.parseInt(parts[i]);
                }
                base6.add(p);
            }
        }
    }

    static void writeSolution(String filename, List<int[]> sol) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("# size = " + sol.size());
            for (int[] p : sol) {
                for (int i = 0; i < p.length; i++) {
                    if (i > 0) pw.print(" ");
                    pw.print(p[i]);
                }
                pw.println();
            }

            // Optional: write chosen insertion positions (commented)
            // pw.println("# chosenPos per input index (i -> k), -1 means not chosen in best:");
            // for (int i = 0; i < chosenPos.length; i++) pw.println(i + " " + chosenPos[i]);
        }
    }

    // -------------------- Candidate generation --------------------

    static void buildCandidates() {
        int m = base6.size();
        cand7 = new int[m][7][7];
        for (int i = 0; i < m; i++) {
            int[] p6 = base6.get(i);
            for (int k = 0; k <= 6; k++) {
                cand7[i][k] = insertSeven(p6, k);
            }
        }
    }

    static int[] insertSeven(int[] p6, int pos) {
        // pos in [0..6], insert 7 at that index
        int[] p7 = new int[7];
        int j = 0;
        for (int i = 0; i < 7; i++) {
            if (i == pos) {
                p7[i] = 7;
            } else {
                p7[i] = p6[j++];
            }
        }
        return p7;
    }

    // -------------------- Backtracking --------------------

    static void dfs(int i) {
        if (current.size() > bestSize) {
            bestSize = current.size();
            best = new ArrayList<>(current.size());
            for (int[] p : current) best.add(p.clone());
            System.out.println("New best: " + bestSize + " (reached i=" + i + ")");

            // EARLY STOP: full U(7,2) achieved (size == 720 if you manage all of them)
            if (bestSize == base6.size()) {
                throw new RuntimeException("DONE");
            }
        }

        if (i >= base6.size()) return;

        // Try each insertion position for this i-th base permutation
        for (int k = 0; k <= 6; k++) {
            int[] candidate = cand7[i][k];

            if (fits(candidate)) {
                current.add(candidate);
                chosenPos[i] = k;

                invPositions.add(inversePositions(candidate));

                dfs(i + 1);

                current.remove(current.size() - 1);
                chosenPos[i] = -1;
                invPositions.remove(invPositions.size() - 1);
            }
        }

        // Same behavior as your Ulam6From5 version:
        // we do NOT “skip” i — if none of the 7 insertions work, we backtrack.
    }

    static boolean fits(int[] cand) {
        // Must have Ulam distance >= 2 to every permutation already in current.
        // n=7, so UlamDist >= 2 <=> LCS <= 5.
        for (int idx = 0; idx < current.size(); idx++) {
            int[] invQ = invPositions.get(idx);
            int lcs = lcsLengthPermutationVsPermutationUsingInv(cand, invQ);
            int d = 7 - lcs;
            if (d < 2) return false;
        }
        return true;
    }

    // -------------------- Ulam distance helpers --------------------

    static int[] inversePositions(int[] perm) {
        // values are 1..7
        int[] inv = new int[8]; // ignore 0
        for (int i = 0; i < perm.length; i++) {
            inv[perm[i]] = i;
        }
        return inv;
    }

    static int lcsLengthPermutationVsPermutationUsingInv(int[] p, int[] invQ) {
        // For permutations: LCS(p,q) = LIS( invQ[p[0]], invQ[p[1]], ... )
        int n = p.length; // 7
        int[] seq = new int[n];
        for (int i = 0; i < n; i++) seq[i] = invQ[p[i]];
        return lisLength(seq);
    }

    static int lisLength(int[] a) {
        // O(n log n) LIS length (strictly increasing)
        int n = a.length;
        int[] tail = new int[n];
        int len = 0;
        for (int x : a) {
            int pos = Arrays.binarySearch(tail, 0, len, x);
            if (pos < 0) pos = -(pos + 1);
            tail[pos] = x;
            if (pos == len) len++;
        }
        return len;
    }
}
