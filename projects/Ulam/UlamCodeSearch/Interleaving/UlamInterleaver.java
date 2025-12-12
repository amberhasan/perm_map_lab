// Source code updated to use cyclic groups instead of full permutations.
// Construction directly follows Theorem 17 guarantee (no greedy filter needed).
// Author: Amber’s modified version

import java.util.*;

public class UlamInterleaver {
    static int s;        // elements per class
    static int t;        // number of translocations
    static int n;        // total length = s * (2t+1)
    static int tracks;   // number of classes = 2t+1

    static List<List<Integer>> classes = new ArrayList<>();

    public static void main(String[] args) {
        // Read parameters
        if (args.length >= 2) {
            try {
                s = Integer.parseInt(args[0]);
                t = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: java UlamInterleaver <s> <t>");
                System.exit(1);
            }
        } else {
            Scanner sc = new Scanner(System.in);
            System.out.print("Enter s (length of each class): ");
            s = sc.nextInt();
            System.out.print("Enter t (# translocations to correct): ");
            t = sc.nextInt();
            sc.close();
        }

        tracks = 2 * t + 1;
        n = s * tracks;
        int minTrackHam = 4 * t + 1;

        System.out.println("Parameters: s=" + s + ", t=" + t +
                ", tracks=" + tracks + ", n=" + n);
        System.out.println("Requirement: s >= 4t+1 = " + minTrackHam);

        if (s < minTrackHam) {
            System.err.println("❌ Error: s is too small. Need s >= " + minTrackHam +
                    " to guarantee Ulam distance bound by Theorem 17.");
            System.exit(1);
        }

        System.out.println("Within each Ci: Hamming distance = s = " + s +
                " (OK since s >= " + minTrackHam + ")");
        System.out.println("Theorem 17 guarantee: d_U(C) >= " + (2 * t + 1));

        // Partition into classes P1..P(tracks)
        partitionClasses();

        // Build cyclic groups for each class
        ArrayList<List<List<Integer>>> groups = new ArrayList<>();
        for (List<Integer> cls : classes) {
            groups.add(generateCyclicGroup(cls));
        }

        // Compute expected code size
        long sizeC = 1;
        for (List<List<Integer>> g : groups) sizeC *= g.size();
        System.out.println("Code size |C| = " + sizeC + " (expected " + s + "^" + tracks + ")");

        // Example: generate the first two codewords and their Ulam distance
        int[] code1 = interleave(pickFirstFromEach(groups));
        int[] code2 = interleave(pickShiftedFromEach(groups));
        System.out.println("Example codeword 1: " + Arrays.toString(code1));
        System.out.println("Example codeword 2: " + Arrays.toString(code2));
        System.out.println("Ulam distance between them = " + ulamDistance(code1, code2));

        // Write full code to file
        //TODO: is this correct? I think it is. 
        String filename = "output_" + "n" + n + "_" + "d" + (2*t +1) + "_" + "s" + s + "_" + "t" + t + ".txt";
        writeCodeToFile(filename, groups);
        System.out.println("✅ Wrote full code to " + filename);
    }

    // Partition numbers 1..n into tracks classes
    static void partitionClasses() {
        for (int i = 0; i < tracks; i++) classes.add(new ArrayList<>());
        for (int k = 1; k <= n; k++) {
            int idx = (k - 1) % tracks;
            classes.get(idx).add(k);
        }
        System.out.println("Partitioned classes:");
        for (int i = 0; i < classes.size(); i++) {
            System.out.println("P" + (i + 1) + " = " + classes.get(i));
        }
    }

    // Generate cyclic shifts for a base list
    static List<List<Integer>> generateCyclicGroup(List<Integer> base) {
        List<List<Integer>> group = new ArrayList<>();
        int size = base.size();
        for (int shift = 0; shift < size; shift++) {
            List<Integer> rotated = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                rotated.add(base.get((i + shift) % size));
            }
            group.add(rotated);
        }
        return group;
    }

    // Interleave chosen permutations
    static int[] interleave(List<List<Integer>> chosen) {
        int k = chosen.size(), s = chosen.get(0).size();
        int[] merged = new int[k * s];
        int idx = 0;
        for (int pos = 0; pos < s; pos++) {
            for (int i = 0; i < k; i++) {
                merged[idx++] = chosen.get(i).get(pos);
            }
        }
        return merged;
    }

    // Example helpers
    static List<List<Integer>> pickFirstFromEach(List<List<List<Integer>>> groups) {
        List<List<Integer>> out = new ArrayList<>();
        for (List<List<Integer>> g : groups) out.add(g.get(0));
        return out;
    }

    static List<List<Integer>> pickShiftedFromEach(List<List<List<Integer>>> groups) {
        List<List<Integer>> out = new ArrayList<>();
        int shift = 1;
        for (List<List<Integer>> g : groups) {
            out.add(g.get(shift % g.size())); // pick different shift
            shift++;
        }
        return out;
    }

    // Ulam distance via LCS
    static int ulamDistance(int[] a, int[] b) {
        int n = a.length, m = b.length;
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a[i - 1] == b[j - 1]) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return n - dp[n][m];
    }

    // Write all interleavings to file (s^tracks lines)
    static void writeCodeToFile(String filename, List<List<List<Integer>>> groups) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(filename)) {
            buildAndWrite(groups, 0, new ArrayList<>(), out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void buildAndWrite(List<List<List<Integer>>> groups, int depth,
                              List<List<Integer>> current, java.io.PrintWriter out) {
        if (depth == groups.size()) {
            int[] cw = interleave(current);
            for (int i = 0; i < cw.length; i++) {
                out.print(cw[i]);
                if (i < cw.length - 1) out.print(" ");
            }
            out.println();
        } else {
            for (List<Integer> choice : groups.get(depth)) {
                current.add(choice);
                buildAndWrite(groups, depth + 1, current, out);
                current.remove(current.size() - 1);
            }
        }
    }
}
