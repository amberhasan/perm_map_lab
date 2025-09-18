import java.util.*;
import java.io.*;

public class UlamCodeSearch {
    static int n;
    static int d;
    static boolean useCache = false;

    static List<int[]> permutations = new ArrayList<>();
    static int maxCliqueSize = 0;
    static List<Integer> bestClique = new ArrayList<>();

    // Optional neighbor cache (lazy fill)
    static Map<Integer, Set<Integer>> neighborCache = new HashMap<>();

    public static void main(String[] args) {
        // Parse input with optional -c flag for caching
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        if (argList.contains("-c")) {
            useCache = true;
            argList.remove("-c");
            System.out.println("Neighbor caching ENABLED.");
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

        System.out.println("Running UlamCodeSearch with n=" + n + " and d=" + d);

        generatePermutations();
        System.out.println("Generated " + permutations.size() + " permutations.");
        findMaxClique();
        System.out.println("\n✅ Search finished. Largest clique size: " + maxCliqueSize);
        System.out.println("Final clique permutations:");
        for (int idx : bestClique) {
            System.out.println(Arrays.toString(permutations.get(idx)));
        }
    }

    // --- Step 1: Generate permutations ---
    static void generatePermutations() {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = i + 1;
        permute(arr, 0);
    }

    static void permute(int[] arr, int l) {
        if (l == arr.length) {
            permutations.add(arr.clone());
            return;
        }
        for (int i = l; i < arr.length; i++) {
            swap(arr, l, i);
            permute(arr, l + 1);
            swap(arr, l, i);
        }
    }

    static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // --- Step 2: Neighbor generation (with optional caching) ---
    static Set<Integer> getNeighbors(int idx) {
        if (useCache && neighborCache.containsKey(idx)) {
            return neighborCache.get(idx);
        }

        Set<Integer> neighbors = new HashSet<>();
        int[] base = permutations.get(idx);
        for (int j = 0; j < permutations.size(); j++) {
            if (j == idx) continue;
            if (ulamDistance(base, permutations.get(j)) >= d) {
                neighbors.add(j);
            }
        }

        if (useCache) {
            neighborCache.put(idx, neighbors);
        }
        return neighbors;
    }

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
        return n - dp[n][n];
    }

    // --- Step 3: Bron–Kerbosch algorithm using neighbors on demand ---
    static void findMaxClique() {
        Set<Integer> allVertices = new HashSet<>();
        for (int i = 0; i < permutations.size(); i++) allVertices.add(i);
        bronKerbosch(new HashSet<>(), allVertices, new HashSet<>());
    }

    static void bronKerbosch(Set<Integer> R, Set<Integer> P, Set<Integer> X) {
        if (P.isEmpty() && X.isEmpty()) {
            if (R.size() > maxCliqueSize) {
                maxCliqueSize = R.size();
                bestClique = new ArrayList<>(R);
                System.out.println("\nNew max clique found! Size: " + maxCliqueSize);
                for (int idx : bestClique) {
                    System.out.println(Arrays.toString(permutations.get(idx)));
                }
                System.out.println("------");
                saveCliqueToFile(bestClique, maxCliqueSize);
            }
            return;
        }

        Integer pivot = !P.isEmpty() ? P.iterator().next() : null;
        Set<Integer> pivotNeighbors = (pivot != null) ? getNeighbors(pivot) : Collections.emptySet();

        Set<Integer> loopSet = new HashSet<>(P);
        loopSet.removeAll(pivotNeighbors);

        for (Integer v : loopSet) {
            Set<Integer> newR = new HashSet<>(R);
            newR.add(v);

            Set<Integer> newP = new HashSet<>(P);
            newP.retainAll(getNeighbors(v));

            Set<Integer> newX = new HashSet<>(X);
            newX.retainAll(getNeighbors(v));

            bronKerbosch(newR, newP, newX);

            P.remove(v);
            X.add(v);
        }
    }

    static void saveCliqueToFile(List<Integer> clique, int size) {
        String filename = String.format("clique_n%d_d%d_size%d.txt", n, d, size);
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("Clique size: " + size);
            for (int idx : clique) {
                out.println(Arrays.toString(permutations.get(idx)));
            }
            System.out.println("Saved clique to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing clique to file: " + e.getMessage());
        }
    }
}