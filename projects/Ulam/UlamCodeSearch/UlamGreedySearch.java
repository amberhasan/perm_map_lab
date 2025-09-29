import java.util.*;
import java.io.*;

public class UlamGreedySearch {
    static int n; // permutation length
    static int d; // minimum Ulam distance
    static int randomSeedCount = 0; // number of random permutations to pick initially
    static List<int[]> solutionSet = new ArrayList<>(); // stores the final set of permutations

    public static void main(String[] args) {
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

        // --- Infinite loop: keep running greedy search ---
        int run = 1;
        while (true) {
            System.out.println("\n==========================");
            System.out.println("🔁 Run #" + run + " starting...");
            System.out.println("==========================");

            // Reset solution set each iteration
            solutionSet.clear();

            // Main algorithm
            greedySearch();

            // Print and save result
            System.out.println("\n✅ Greedy search finished. Found set size: " + solutionSet.size());
            for (int[] perm : solutionSet) {
                System.out.println(Arrays.toString(perm));
            }
            saveSolutionToFile();

            System.out.println("Run #" + run + " complete. Sleeping 1s before next run. Press Ctrl+C to stop.\n");

            try {
                Thread.sleep(1000); // prevent CPU overload between runs
            } catch (InterruptedException e) {
                break;
            }

            run++;
        }
    }

    static void greedySearch() {
        // Generate all permutations first
        List<int[]> allPerms = generateAllPermutations();

        // --- Step 1: Random seeding ---
        if (randomSeedCount > 0) {
            // Shuffle permutation list and pick k random candidates for initial solutionSet
            Collections.shuffle(allPerms, new Random());
            Iterator<int[]> iter = allPerms.iterator();
            while (iter.hasNext() && solutionSet.size() < randomSeedCount) {
                int[] candidate = iter.next();
                if (isGoodCandidate(candidate)) { // check distance to all chosen perms
                    solutionSet.add(candidate.clone());
                    iter.remove();
                }
            }
            // Sort remaining permutations back into lex order for reproducible greedy step
            allPerms.sort(UlamGreedySearch::lexCompare);
        }

        // --- Step 2: Greedy lexicographic construction ---
        for (int[] perm : allPerms) {
            if (isGoodCandidate(perm)) {
                solutionSet.add(perm.clone());
            }
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
