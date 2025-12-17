import java.io.*;
import java.util.*;

public class Ulam7Direct {

    static int n = 8;
    static List<int[]> code = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        String outFile = (args.length >= 1) ? args[0] : "U8_d2_Levenshtein.txt";

        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i + 1;

        permute(a, 0);

        System.out.println("n = " + n);
        System.out.println("Generated |C| = " + code.size() + " (expected " + factorial(n - 1) + ")");
        writeToFile(code, outFile);
        System.out.println("Wrote: " + outFile);
    }

    // Generate all permutations, filter by Levenshtein descent congruence
    static void permute(int[] a, int i) {
        if (i == a.length) {
            if (inLevenshteinCode(a)) code.add(a.clone());
            return;
        }
        for (int j = i; j < a.length; j++) {
            swap(a, i, j);
            permute(a, i + 1);
            swap(a, i, j);
        }
    }

    // Levenshtein condition for U(n,2):
    // Let z(i)=1 iff a[i-1] > a[i] (a descent at position i), for i=1..n-1.
    // Keep permutation iff n divides sum_{i=1..n-1} i*z(i).
    static boolean inLevenshteinCode(int[] p) {
        int sum = 0;
        for (int i = 1; i <= n - 1; i++) {
            if (p[i - 1] > p[i]) sum += i;
        }
        return (sum % n) == 0;
    }

    static void writeToFile(List<int[]> A, String filename) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            for (int[] p : A) {
                for (int i = 0; i < p.length; i++) {
                    out.print(p[i]);
                    if (i + 1 < p.length) out.print(" ");
                }
                out.println();
            }
        }
    }

    static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    static int factorial(int x) {
        int r = 1;
        for (int i = 2; i <= x; i++) r *= i;
        return r;
    }
}
