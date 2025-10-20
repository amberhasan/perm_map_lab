import java.util.*;
import java.io.*;

public class UlamGreedySearch_random {
    static int n; // permutation length
    static int d; // minimum Ulam distance
    static int randomSeedCount = 0; // number of random permutations to pick initially
    static List<int[]> solutionSet = new ArrayList<>(); // stores the current solution set
    static Random rng; // global random generator

    public static void main(String[] args) {
        // --- Parse input ---
        List<String> argList = new ArrayList<>(Arrays.asList(args));

        // Optional: -seed <val> sets fixed RNG seed
        long seed = System.currentTimeMillis(); // default: time-based randomness
        int seedIndex = argList.indexOf("-seed");
        if (seedIndex != -1 && seedIndex + 1 < argList.size()) {
            seed = Long.parseLong(argList.get(seedIndex + 1));
            argList.remove(seedIndex + 1);
            argList.remove(seedIndex);
            System.out.println("Using fixed seed: " + seed);
        } else {
            System.out.println("Using time-based seed: " + seed);
        }
        rng = new Random(seed);

        // Optional: -r k enables random seeding with k permutations
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
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter n (permutation length): ");
            n = scanner.nextInt();
            System.out.print("Enter d (minimum Ulam distance): ");
            d = scanner.nextInt();
        }

        System.out.println("Running UlamGreedySearch with n=" + n + " and d=" + d);

        greedySearch();
    }

    static void greedySearch() {
        List<int[]> allPerms = generateAllPermutations();
        List<int[]> bestSolution = new ArrayList<>();

        while (true) {
            solutionSet.clear();
            List<int[]> candidatePerms = new ArrayList<>(allPerms);

            // Shuffle once per pass to inject randomness
            Collections.shuffle(candidatePerms, rng);

            // --- Step 1: Seed phase (pick r random good candidates) ---
            Iterator<int[]> iter = candidatePerms.iterator();
            while (iter.hasNext() && solutionSet.size() < randomSeedCount) {
                int[] candidate = iter.next();
                if (isGoodCandidate(candidate)) {
                    solutionSet.add(candidate.clone());
                }
                iter.remove();
            }

            // --- Step 2: Greedy phase but on randomized order ---
            iter = candidatePerms.iterator();
            while (iter.hasNext()) {
                int[] perm = iter.next();
                if (isGoodCandidate(perm)) {
                    solutionSet.add(perm.clone());
                }
                iter.remove();
            }

            // --- Compare with best so far ---
            if (solutionSet.size() > bestSolution.size()) {
                bestSolution.clear();
                for (int[] perm : solutionSet) bestSolution.add(perm.clone());
                System.out.println("\n🎉 New best solution found! Size: " + bestSolution.size());
                saveBestSolutionToFile(bestSolution);
            }

            System.out.println("Current solution size: " + solutionSet.size() +
                               " | Best so far: " + bestSolution.size());

            if (candidatePerms.isEmpty()) {
                candidatePerms = generateAllPermutations();
            }
        }
    }

    static void saveBestSolutionToFile(List<int[]> bestSolution) {
        String filename = String.format("best_greedy_n%d_d%d_size%d.txt", n, d, bestSolution.size());
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("Best solution size: " + bestSolution.size());
            for (int[] perm : bestSolution) {
                out.println(Arrays.toString(perm));
            }
            System.out.println("💾 Saved new best solution to " + filename);
        } catch (IOException e) {
            System.err.println("Error saving best solution: " + e.getMessage());
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

    // Checks if a permutation can be added to solutionSet (distance >= d from all existing ones)
    static boolean isGoodCandidate(int[] candidate) {
        for (int[] existing : solutionSet) {
            if (ulamDistance(candidate, existing) < d) {
                return false;
            }
        }
        return true;
    }

    static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // Compute Ulam distance using LCS dynamic programming
    static int ulamDistance(int[] a, int[] b) {
        int[][] dp = new int[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i - 1] == b[j - 1])
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                else
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        return n - dp[n][n]; // n - LCS length
    }

    // Save current greedy solution to a file
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
