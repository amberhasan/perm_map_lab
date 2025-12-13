import java.util.*;
import java.io.*;

public class UlamDistanceChecker {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java UlamDistanceChecker <filename>");
            return;
        }

        String filename = args[0];
        List<int[]> solution = readPermutationsFromFile(filename);

        if (solution.isEmpty()) {
            System.out.println("No permutations loaded from file.");
            return;
        }

        int n = solution.get(0).length;
        int d = computeMinDistance(solution, n);

        System.out.println("n = " + n + ", d = " + d);
    }

    // -------------------- FILE READER --------------------

    static List<int[]> readPermutationsFromFile(String filename) {
        List<int[]> perms = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("/*") || line.startsWith("//") || line.startsWith("#")) continue;

                // allow "(1,2,3)", "[1,2,3]", or "1 2 3"
                line = line.replaceAll("[\\[\\]\\(\\),]", " ");
                String[] parts = line.trim().split("\\s+");

                int[] perm = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    perm[i] = Integer.parseInt(parts[i]);
                }
                perms.add(perm);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return perms;
    }

    // -------------------- MIN DISTANCE --------------------

    static int computeMinDistance(List<int[]> solution, int n) {
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < solution.size(); i++) {
            for (int j = i + 1; j < solution.size(); j++) {
                int dist = ulamDistance(solution.get(i), solution.get(j));
                minDist = Math.min(minDist, dist);
            }
        }
        return (minDist == Integer.MAX_VALUE ? 0 : minDist);
    }

    // -------------------- CORRECT ULAM DISTANCE --------------------

    static int ulamDistance(int[] a, int[] b) {
        int n = a.length;

        // inverse of a
        int[] invA = new int[n + 1];
        for (int i = 0; i < n; i++) {
            invA[a[i]] = i + 1;
        }

        // build pi^{-1} o sigma
        int[] mapped = new int[n];
        for (int i = 0; i < n; i++) {
            mapped[i] = invA[b[i]];
        }

        int lis = lengthOfLIS(mapped);
        return n - lis;
    }

    // -------------------- LIS (O(n log n)) --------------------

    static int lengthOfLIS(int[] arr) {
        int[] tails = new int[arr.length];
        int size = 0;

        for (int x : arr) {
            int i = Arrays.binarySearch(tails, 0, size, x);
            if (i < 0) i = -(i + 1);
            tails[i] = x;
            if (i == size) size++;
        }
        return size;
    }
}
