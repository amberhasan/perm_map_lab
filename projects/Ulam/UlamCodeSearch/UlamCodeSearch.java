import java.util.*;
import java.io.*;

public class UlamCodeSearch {
    static int n;
    static int d;
    static List<int[]> permutations = new ArrayList<>();
    static Map<Integer, List<Integer>> graph = new HashMap<>();
    static int maxCliqueSize = 0;
    static List<Integer> bestClique = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length >= 2) {
            try {
                n = Integer.parseInt(args[0]);
                d = Integer.parseInt(args[1]);
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
        buildGraph();
        findMaxClique();
        System.out.println("\nSearch finished. Largest clique size: " + maxCliqueSize);
        System.out.println("Final clique permutations:");
        for (int idx : bestClique) {
            System.out.println(Arrays.toString(permutations.get(idx)));
        }
    }

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

    static void buildGraph() {
        int total = permutations.size();
        for (int i = 0; i < total; i++) {
            graph.put(i, new ArrayList<>());
        }
        for (int i = 0; i < total; i++) {
            for (int j = i + 1; j < total; j++) {
                if (ulamDistance(permutations.get(i), permutations.get(j)) >= d) {
                    graph.get(i).add(j);
                    graph.get(j).add(i);
                }
            }
        }
        System.out.println("Graph built with " + total + " permutations.");
    }

    static int ulamDistance(int[] a, int[] b) {
        int[][] dp = new int[n+1][n+1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (a[i-1] == b[j-1])
                    dp[i][j] = dp[i-1][j-1] + 1;
                else
                    dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
            }
        }
        return n - dp[n][n];
    }

    static void findMaxClique() {
        bronKerbosch(new HashSet<>(), new HashSet<>(graph.keySet()), new HashSet<>());
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
        Set<Integer> neighbors = (pivot != null) ? new HashSet<>(graph.get(pivot)) : Collections.emptySet();

        Set<Integer> loopSet = new HashSet<>(P);
        loopSet.removeAll(neighbors);

        for (Integer v : loopSet) {
            Set<Integer> newR = new HashSet<>(R);
            newR.add(v);

            Set<Integer> newP = new HashSet<>(P);
            newP.retainAll(graph.get(v));

            Set<Integer> newX = new HashSet<>(X);
            newX.retainAll(graph.get(v));

            bronKerbosch(newR, newP, newX);

            P.remove(v);
            X.add(v);
        }
    }

    static void saveCliqueToFile(List<Integer> clique, int size) {
        String filename = "clique_" + size + ".txt";
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