import java.io.*;
import java.util.*;

/**
 * Build a large Ulam-distance-2 code in S_6 by taking the 120 permutations of length 5
 * (in a fixed input order) and inserting the symbol 6 into each one.
 *
 * Constraint: For the chosen length-6 permutations, every pair must have Ulam distance >= 2.
 *
 * Ulam distance for permutations p,q of length n:
 *   d_U(p,q) = n - LCS(p,q)
 * For permutations, LCS(p,q) = LIS( posInQ[p[0]], posInQ[p[1]], ... )
 */
public class Ulam6From5Backtrack {

    // Input permutations of length 5 (size should be 120)
    static List<int[]> base5 = new ArrayList<>();

    // For each i, there are 6 candidates: base5[i] with 6 inserted at position k
    static int[][][] cand6; // [i][k][6]

    // Best solution found
    static List<int[]> best = new ArrayList<>();
    static int bestSize = 0;

    // Current partial solution
    static List<int[]> current = new ArrayList<>();

    // Track which insertion position we chose for each i in current (for debugging)
    static int[] chosenPos; // chosenPos[i] = k used when we accepted i-th base perm, else -1

    // Small speedup: store inverse position arrays for the current solution perms
    // invPositions.get(t)[value] = position of value in current.get(t)
    static List<int[]> invPositions = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Ulam6From5Backtrack <perms5_file> [output_file]");
            System.out.println("Example: java Ulam6From5Backtrack perms5.txt out_U6_d2.txt");
            return;
        }
        String inFile = args[0];
        String outFile = (args.length >= 2) ? args[1] : "U6_d2_solution.txt";

        readPerms5(inFile);

        if (base5.size() == 0) {
            System.out.println("No permutations loaded. Check file format.");
            return;
        }
        if (base5.get(0).length != 5) {
            System.out.println("Input permutations must be length 5.");
            return;
        }

        System.out.println("Loaded " + base5.size() + " permutations of length 5.");

        buildCandidates();

        chosenPos = new int[base5.size()];
        Arrays.fill(chosenPos, -1);

        // Backtracking search in the fixed order 0..119
        try {
            dfs(0);
        } catch (RuntimeException e) {
            if (!e.getMessage().equals("DONE")) throw e;
        }


        System.out.println("\nBEST SIZE FOUND = " + bestSize);
        writeSolution(outFile, best);
        System.out.println("Wrote best solution to: " + outFile);
    }

    // -------------------- IO --------------------

    static void readPerms5(String filename) throws IOException {
        base5.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Allow formats like: "1 2 3 4 5" or "[1, 2, 3, 4, 5]"
                line = line.replace("[", " ").replace("]", " ").replace(",", " ");
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 5) continue; // ignore junk lines

                int[] p = new int[5];
                for (int i = 0; i < 5; i++) {
                    p[i] = Integer.parseInt(parts[i]);
                }
                base5.add(p);
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
        }
    }

    // -------------------- Candidate generation --------------------

    static void buildCandidates() {
        int m = base5.size();
        cand6 = new int[m][6][6];
        for (int i = 0; i < m; i++) {
            int[] p5 = base5.get(i);
            for (int k = 0; k <= 5; k++) {
                cand6[i][k] = insertSix(p5, k);
            }
        }
    }

    static int[] insertSix(int[] p5, int pos) {
        // pos in [0..5], insert 6 at that index
        int[] p6 = new int[6];
        int j = 0;
        for (int i = 0; i < 6; i++) {
            if (i == pos) {
                p6[i] = 6;
            } else {
                p6[i] = p5[j++];
            }
        }
        return p6;
    }

    // -------------------- Backtracking --------------------

    static void dfs(int i) {
        if (current.size() > bestSize) {
            bestSize = current.size();
            best = new ArrayList<>(current.size());
            for (int[] p : current) best.add(p.clone());
            System.out.println("New best: " + bestSize + " (reached i=" + i + ")");

            // EARLY STOP: full U(6,2) achieved
            if (bestSize == base5.size()) {
                throw new RuntimeException("DONE");
            }
        }

        // If we've processed all base perms, stop
        if (i >= base5.size()) return;

        // Try each insertion position for this i-th base permutation
        for (int k = 0; k <= 5; k++) {
            int[] candidate = cand6[i][k];

            if (fits(candidate)) {
                // choose
                current.add(candidate);
                chosenPos[i] = k;

                // cache inverse positions for speed
                invPositions.add(inversePositions(candidate));

                dfs(i + 1);

                // unchoose
                current.remove(current.size() - 1);
                chosenPos[i] = -1;
                invPositions.remove(invPositions.size() - 1);
            }
        }

        // Important: Hal’s text suggests we proceed in order and *try* to include each one.
        // This code enforces that: if none of the 6 insertions work at i, we must backtrack.
        // We do NOT “skip” i, because that would violate the “at the i-th iteration size i” phrasing.
    }

    static boolean fits(int[] cand) {
        // Must have Ulam distance >= 2 to every permutation already in current.
        // n=6, so UlamDist >= 2 <=> LCS <= 4.
        for (int idx = 0; idx < current.size(); idx++) {
            int[] invQ = invPositions.get(idx);
            int lcs = lcsLengthPermutationVsPermutationUsingInv(cand, invQ);
            int d = 6 - lcs;
            if (d < 2) return false;
        }
        return true;
    }

    // -------------------- Ulam distance helpers --------------------

    static int[] inversePositions(int[] perm) {
        // values are 1..6
        int[] inv = new int[7]; // ignore 0
        for (int i = 0; i < perm.length; i++) {
            inv[perm[i]] = i;
        }
        return inv;
    }

    static int lcsLengthPermutationVsPermutationUsingInv(int[] p, int[] invQ) {
        // For permutations: LCS(p,q) = LIS( invQ[p[0]], invQ[p[1]], ... )
        int n = p.length; // 6
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
