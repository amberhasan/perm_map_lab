import java.util.*;
import java.io.*;

/**
 * Usage:
 *   java UlamInterleaverSearch n d [--samples=20000] [--seed=42]
 *
 * Notes:
 * - Tries all valid t (0..(n-1)/2 with n % (2t+1)==0). s = n/(2t+1).
 * - For each (s,t), generates 'samples' random interleaved permutations by
 *   shuffling each congruence class once per sample and merging.
 * - Greedily keeps permutations with Ulam distance >= d from all kept ones.
 * - Ulam distance uses O(n log n) LIS (patience sorting) on mapped indices.
 */
public class UlamInterleaverSearch {
    static int n, d;
    static int bestS = -1, bestT = -1;
    static List<int[]> bestPermutationArray = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java UlamInterleaverSearch n d [--samples=20000] [--seed=42]");
            System.exit(1);
        }
        n = Integer.parseInt(args[0]);
        d = Integer.parseInt(args[1]);

        long samples = 20000;
        long seed = 42L;
        for (int i = 2; i < args.length; i++) {
            if (args[i].startsWith("--samples=")) {
                samples = Long.parseLong(args[i].substring("--samples=".length()));
            } else if (args[i].startsWith("--seed=")) {
                seed = Long.parseLong(args[i].substring("--seed=".length()));
            }
        }

        // Prebuild the 1..n array once
        int[] universe = new int[n];
        for (int i = 0; i < n; i++) universe[i] = i + 1;

        // Try all valid (s,t)
        for (int t = 0; t <= (n - 1) / 2; t++) {
            int numClasses = 2 * t + 1;
            if (n % numClasses != 0) continue; // s must be integer
            int s = n / numClasses;

            List<int[]> candidate = sampleGreedyForST(universe, s, t, samples, seed);
            System.out.printf("s=%d, t=%d -> kept %d permutations (from %d samples)%n",
                    s, t, candidate.size(), samples);

            if (candidate.size() > bestPermutationArray.size()) {
                bestPermutationArray = candidate;
                bestS = s;
                bestT = t;
            }
        }
        savePermutationArrayToFile();
    }

    /** Randomized greedy: generate 'samples' interleaved permutations, keep those far enough. */
    static List<int[]> sampleGreedyForST(int[] universe, int s, int t, long samples, long seed) {
        int numClasses = 2 * t + 1;
        // Build class buckets (indices of elements belonging to each class)
        int[][] classes = new int[numClasses][s];
        for (int i = 0; i < n; i++) {
            int idx = i % numClasses;
            classes[idx][i / numClasses] = universe[i];
        }

        // Work buffers to avoid GC churn
        int[] merged = new int[n];

        Random rng = new Random(seed + 31L * numClasses + 1);
        List<int[]> selected = new ArrayList<>();
        // Pre-allocate arrays used for O(n log n) LIS mapping
        int[] posInOther = new int[n + 1]; // value -> position in 'other'
        int[] mapped = new int[n];         // a mapped through 'other' positions

        for (long iter = 0; iter < samples; iter++) {
            // For each class, create a shuffled copy (Fisher-Yates)
            // Then interleave into 'merged'
            for (int classIndex = 0; classIndex < numClasses; classIndex++) {
                shuffleInPlace(classes[classIndex], rng);
            }
            interleaveIntoMerged(classes, numClasses, merged);

            // Greedy accept if far enough from all previously kept
            boolean ok = true;
            for (int[] kept : selected) {
                if (ulamDistanceFast(merged, kept, posInOther, mapped) < d) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                selected.add(Arrays.copyOf(merged, n));
            }
        }
        return selected;
    }

    /** Fisher–Yates in-place shuffle of an int[] */
    static void shuffleInPlace(int[] a, Random rng) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = a[i]; a[i] = a[j]; a[j] = tmp;
        }
    }

    /** Interleave class arrays into one permutation: positions i where i % numClasses == c come from classes[c] in order. */
    static void interleaveIntoMerged(int[][] classes, int numClasses, int[] out) {
        int s = classes[0].length;
        // For each class, write its s items into positions congruent to classIndex mod numClasses
        for (int classIndex = 0; classIndex < numClasses; classIndex++) {
            int pos = 0;
            for (int i = classIndex; i < out.length; i += numClasses) {
                out[i] = classes[classIndex][pos++];
            }
        }
    }

    /**
     * Ulam distance for permutations in O(n log n).
     * Given two permutations a,b over 1..n:
     * 1) Build posInOther[val] = index of val in b
     * 2) Map a -> sequence of indices posInOther[a[i]]
     * 3) Ulam = n - LIS(mapped)
     */
    static int ulamDistanceFast(int[] a, int[] b, int[] posInOther, int[] mapped) {
        final int n = a.length;
        for (int i = 0; i < n; i++) posInOther[b[i]] = i;
        for (int i = 0; i < n; i++) mapped[i] = posInOther[a[i]];
        int lis = lisLength(mapped);
        return n - lis;
    }

    /** Patience sorting LIS length in O(n log n) for int[] */
    static int lisLength(int[] arr) {
        int[] tails = new int[arr.length];
        int size = 0;
        for (int x : arr) {
            int i = Arrays.binarySearch(tails, 0, size, x);
            if (i < 0) i = -i - 1;
            tails[i] = x;
            if (i == size) size++;
        }
        return size;
    }

    /** Save the best permutation array to a file */
    static void savePermutationArrayToFile() {
        String filename = String.format("best_PA_n%d_d%d_s%d_t%d_size%d.txt",
                n, d, bestS, bestT, bestPermutationArray.size());
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            for (int[] perm : bestPermutationArray) {
                // Print as space-separated values
                for (int i = 0; i < perm.length; i++) {
                    if (i > 0) out.print(' ');
                    out.print(perm[i]);
                }
                out.println();
            }
            System.out.println("Saved best solution to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }
}
