import java.util.*;
import java.io.*;

public class UlamGreedySearch2 {
    static int n; // permutation length
    static int d; // minimum Ulam distance
    static int randomSeedCount = 0; // number of random permutations to pick initially
    static List<int[]> solutionSet = new ArrayList<>(); // stores the final set of permutations

    public static void main(String[] args) {
        solutionSet = new ArrayList<>(); // stores the final set of permutations

        // --- Parse input ---
        // Optional: -r k enables random seeding with k permutations
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        int rIndex = argList.indexOf("-r");
        if (rIndex != -1 && rIndex + 1 < argList.size()) {
            randomSeedCount = Integer.parseInt(argList.get(rIndex + 1));
            argList.remove(rIndex + 1);
            argList.remove(rIndex);
            System.out.println("Random seeding ENABLED with " + randomSeedCount + " random permutations.");
        }

        if (argList.size() >= 2) {
            try {
                n = Integer.parseInt(argList.get(0));
                d = Integer.parseInt(argList.get(1));
            } catch (NumberFormatException e) {
                System.err.println("Error: n and d must be integers.");
                System.exit(1);
            }
        } else {
            // If no args given, ask interactively
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter n (permutation length): ");
            n = scanner.nextInt();
            System.out.print("Enter d (minimum Ulam distance): ");
            d = scanner.nextInt();
        }

        System.out.println("Running UlamGreedySearch with n=" + n + " and d=" + d);

        // Main algorithm
        greedySearch();

        // Print and save result
        System.out.println("\n✅ Greedy search finished. Found set size: " + solutionSet.size());
        for (int[] perm : solutionSet) {
            System.out.println(Arrays.toString(perm));
        }
        saveSolutionToFile();
    }
    // Returns the next lexicographic permutation of arr, or false if it was the last one
    static boolean nextPermutation(int[] arr) {
        int i = arr.length - 2;
        while (i >= 0 && arr[i] > arr[i + 1]) i--;
        if (i < 0) return false;
        int j = arr.length - 1;
        while (arr[j] < arr[i]) j--;
        swap(arr, i, j);
        reverse(arr, i + 1, arr.length - 1);
        return true;
    }

    static void reverse(int[] arr, int l, int r) {
        while (l < r) {
            swap(arr, l++, r--);
        }
    }
    static void greedySearch() {
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i + 1;

        // Step 1: random seeding (optional, can shuffle a few initial perms)
        Random rng = new Random();
        if (randomSeedCount > 0) {
            for (int k = 0; k < randomSeedCount; k++) {
                int[] candidate = perm.clone();
                shuffle(candidate, rng);
                if (isGoodCandidate(candidate))
                    solutionSet.add(candidate.clone());
            }
        }

        // Step 2: lexicographic iteration
        do {
            if (isGoodCandidate(perm))
                solutionSet.add(perm.clone());
        } while (nextPermutation(perm));
    }

    static void shuffle(int[] arr, Random rng) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            swap(arr, i, j);
        }
    }



    // Generate all permutations of [1..n] recursively
    static List<int[]> generateAllPermutations() {
        List<int[]> perms = new ArrayList<>();
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = i + 1;
        generateRecursive(arr, 0, perms);
        return perms;
    }

    static void generateRecursive(int[] arr, int l, List<int[]> perms) {
        if (l == arr.length) {
            perms.add(arr.clone());
            return;
        }
        for (int i = l; i < arr.length; i++) {
            swap(arr, l, i);
            generateRecursive(arr, l + 1, perms);
            swap(arr, l, i);
        }
    }

    // Compare two permutations lexicographically
    static int lexCompare(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        }
        return 0;
    }

    static boolean isGoodCandidate(int[] candidate) {
        for (int[] existing : solutionSet) {
            if (ulamDistanceEarlyStop(candidate, existing, d)) {
                return false;
            }
        }
        return true;
    }
    static boolean ulamDistanceEarlyStop(int[] a, int[] b, int minDistance) {
        int[] inv = new int[n + 1];
        for (int i = 0; i < n; i++) inv[b[i]] = i + 1;
        int[] mapped = new int[n];
        for (int i = 0; i < n; i++) mapped[i] = inv[a[i]];

        int[] bit = new int[n + 2];
        int maxLIS = 0;
        for (int val : mapped) {
            int best = queryBIT(bit, val - 1);
            int cur = best + 1;
            updateBIT(bit, val, cur);
            if (cur > n - minDistance) return true; // early reject
            maxLIS = Math.max(maxLIS, cur);
        }
        return false;
    }


    static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    static int ulamDistance(int[] a, int[] b) {
        // Build inverse of b
        int[] inv = new int[n + 1];
        for (int i = 0; i < n; i++) inv[b[i]] = i + 1;  // 1-indexed

        // Map a through b⁻¹
        int[] mapped = new int[n];
        for (int i = 0; i < n; i++) mapped[i] = inv[a[i]];

        // Fenwick tree for LIS
        int[] bit = new int[n + 2];
        for (int val : mapped) {
            int best = queryBIT(bit, val - 1);
            updateBIT(bit, val, best + 1);
        }
        int lis = queryBIT(bit, n);
        return n - lis; // Ulam = n - LIS
    }

    static int queryBIT(int[] bit, int idx) {
        int res = 0;
        while (idx > 0) {
            res = Math.max(res, bit[idx]);
            idx -= idx & -idx;
        }
        return res;
    }

    static void updateBIT(int[] bit, int idx, int val) {
        while (idx < bit.length) {
            bit[idx] = Math.max(bit[idx], val);
            idx += idx & -idx;
        }
    }

    // Save solution set to a file with descriptive name
    static void saveSolutionToFile() {
        String filename = String.format("greedy_n%d_d%d_size%d.txt", n, d, solutionSet.size());
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("Greedy solution size: " + solutionSet.size());
            for (int[] perm : solutionSet) {
                out.println(Arrays.toString(perm));
            }
            System.out.println("Saved greedy solution to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing greedy solution to file: " + e.getMessage());
        }
    }
}
