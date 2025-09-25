import java.util.*;
import java.io.*;

public class UlamInterleaverSearch {
    static int n, d;
    static List<List<Integer>> bestPermutationArray = new ArrayList<>();
    static int bestS, bestT;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java UlamInterleaverSearch n d");
            System.exit(1);
        }
        n = Integer.parseInt(args[0]);
        d = Integer.parseInt(args[1]);

        // Try all valid (s,t) combinations
        for (int t = 0; t <= (n - 1) / 2; t++) {
            int numClasses = 2 * t + 1;
            if (n % numClasses != 0) continue; // s must be integer

            int s = n / numClasses;
            List<List<Integer>> candidatePA = buildPermutationArray(s, t);

            System.out.printf("s=%d, t=%d -> found %d permutations%n", s, t, candidatePA.size());

            if (candidatePA.size() > bestPermutationArray.size()) {
                bestPermutationArray = candidatePA;
                bestS = s;
                bestT = t;
            }
        }
        savePermutationArrayToFile();
    }

    /**
     * Build all permutations using interleaver structure for given s,t
     * and greedily select a subset with Ulam distance >= d.
     */
    static List<List<Integer>> buildPermutationArray(int s, int t) {
        // Step 1: divide into classes
        List<List<Integer>> classes = new ArrayList<>();
        for (int i = 0; i < (2*t+1); i++) classes.add(new ArrayList<>());
        for (int i = 1; i <= n; i++) {
            int idx = (i-1) % (2*t+1);
            classes.get(idx).add(i);
        }

        // Step 2: generate all permutations for each class
        List<List<List<Integer>>> classPermutations = new ArrayList<>();
        for (List<Integer> cls : classes) {
            classPermutations.add(generateAllPermutations(cls));
        }

        // Step 3: interleave permutations from each class
        List<List<Integer>> allInterleavedPermutations = interleaveClassPermutations(classPermutations);

        // Step 4: greedy selection by Ulam distance
        List<List<Integer>> selectedPermutations = new ArrayList<>();
        for (List<Integer> candidate : allInterleavedPermutations) {
            if (isFarEnough(candidate, selectedPermutations)) {
                selectedPermutations.add(candidate);
            }
        }
        return selectedPermutations;
    }

    /** Generate all permutations of a list using recursion */
    static List<List<Integer>> generateAllPermutations(List<Integer> elements) {
        List<List<Integer>> result = new ArrayList<>();
        permuteRecursive(elements, 0, result);
        return result;
    }

    static void permuteRecursive(List<Integer> arr, int l, List<List<Integer>> result) {
        if (l == arr.size()) {
            result.add(new ArrayList<>(arr));
        } else {
            for (int i = l; i < arr.size(); i++) {
                Collections.swap(arr, i, l);
                permuteRecursive(arr, l+1, result);
                Collections.swap(arr, i, l);
            }
        }
    }

    /** Build all interleavings (Cartesian product of class permutations) */
    static List<List<Integer>> interleaveClassPermutations(List<List<List<Integer>>> classPermutations) {
        List<List<Integer>> results = new ArrayList<>();
        interleaveRecursive(classPermutations, 0, new ArrayList<>(), results);
        return results;
    }

    static void interleaveRecursive(List<List<List<Integer>>> classPermutations,
                                    int depth,
                                    List<List<Integer>> current,
                                    List<List<Integer>> results) {
        if (depth == classPermutations.size()) {
            results.add(mergeClasses(current, classPermutations.size()));
            return;
        }
        for (List<Integer> choice : classPermutations.get(depth)) {
            current.add(choice);
            interleaveRecursive(classPermutations, depth+1, current, results);
            current.remove(current.size()-1);
        }
    }

    /** Merge chosen permutations into a single interleaved permutation */
    static List<Integer> mergeClasses(List<List<Integer>> chosenPerms, int numClasses) {
        List<Integer> merged = new ArrayList<>(Collections.nCopies(n, 0));
        for (int classIndex = 0; classIndex < numClasses; classIndex++) {
            List<Integer> perm = chosenPerms.get(classIndex);
            int pos = 0;
            for (int i = 0; i < n; i++) {
                if (i % numClasses == classIndex) {
                    merged.set(i, perm.get(pos++));
                }
            }
        }
        return merged;
    }

    /** Check if candidate permutation is far enough (Ulam distance >= d) from all chosen ones */
    static boolean isFarEnough(List<Integer> candidate, List<List<Integer>> selectedPermutations) {
        for (List<Integer> existing : selectedPermutations) {
            if (ulamDistance(candidate, existing) < d) {
                return false;
            }
        }
        return true;
    }

    /** Compute Ulam distance = n - LCS length */
    static int ulamDistance(List<Integer> a, List<Integer> b) {
        int[][] dp = new int[n+1][n+1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.get(i-1).equals(b.get(j-1)))
                    dp[i][j] = dp[i-1][j-1] + 1;
                else
                    dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
            }
        }
        return n - dp[n][n];
    }

    /** Save the best permutation array to a file */
    static void savePermutationArrayToFile() {
        String filename = String.format("best_PA_n%d_d%d_s%d_t%d_size%d.txt",
                n, d, bestS, bestT, bestPermutationArray.size());
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            for (List<Integer> perm : bestPermutationArray) {
                out.println(perm);
            }
            System.out.println("Saved best solution to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }
}
