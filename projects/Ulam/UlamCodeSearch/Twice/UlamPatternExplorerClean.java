import java.util.*;
import java.io.*;

public class UlamPatternExplorerClean {

    // --- Compute Ulam distance between two permutations ---
    public static int ulamDistance(int[] p, int[] q) {
        int n = p.length;
        if (n != q.length) throw new IllegalArgumentException("Permutations must be same length.");
        Map<Integer, Integer> pos = new HashMap<>();
        for (int i = 0; i < n; i++) pos.put(q[i], i);
        int[] mapped = new int[n];
        for (int i = 0; i < n; i++) mapped[i] = pos.get(p[i]);
        return n - longestIncreasingSubsequence(mapped);
    }

    private static int longestIncreasingSubsequence(int[] arr) {
        ArrayList<Integer> tail = new ArrayList<>();
        for (int num : arr) {
            int idx = Collections.binarySearch(tail, num);
            if (idx < 0) idx = -(idx + 1);
            if (idx == tail.size()) tail.add(num);
            else tail.set(idx, num);
        }
        return tail.size();
    }

    // --- Helpers ---
    public static int[] interleave(int[] a, int[] b) {
        int[] res = new int[a.length + b.length];
        int i = 0, j = 0, k = 0;
        while (i < a.length || j < b.length) {
            if (i < a.length) res[k++] = a[i++];
            if (j < b.length) res[k++] = b[j++];
        }
        return res;
    }

    public static int[] reverse(int[] a) {
        int[] r = new int[a.length];
        for (int i = 0; i < a.length; i++) r[i] = a[a.length - 1 - i];
        return r;
    }

    public static int[] concat(int[] a, int[] b) {
        int[] res = new int[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    public static int[] rotate(int[] a, int k) {
        int n = a.length;
        int[] r = new int[n];
        for (int i = 0; i < n; i++) r[i] = a[(i + k) % n];
        return r;
    }

    public static int[] parsePerm(String s) {
        return Arrays.stream(s.trim().split(","))
                .map(String::trim)
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static String toStr(int[] arr) {
        return Arrays.toString(arr).replaceAll("[\\[\\]]", "");
    }

    private static void printAndSave(PrintWriter out, String label, int[] x, int[] y) {
        int d = ulamDistance(x, y);
        System.out.printf("%-45s | %3d | %3d%n", label, x.length, d);
        out.println("-----------------------------------------------------------");
        out.println(label);
        out.println("  P1: " + toStr(x));
        out.println("  P2: " + toStr(y));
        out.println("  Length: " + x.length + "   Ulam Distance: " + d);
        out.println();
    }

    // --- MAIN ---
    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter permutation A (comma-separated):");
        int[] A = parsePerm(sc.nextLine());
        System.out.println("Enter permutation A' (comma-separated):");
        int[] Aprime = parsePerm(sc.nextLine());
        int[] AR = reverse(A);
        int[] AprimeR = reverse(Aprime);
        int[] AprimeRot = rotate(Aprime, 1);

        PrintWriter out = new PrintWriter(new FileWriter("ulam_results.txt"));
        out.println("Ulam Pattern Explorer Results");
        out.println("A = " + toStr(A));
        out.println("A' = " + toStr(Aprime));
        out.println();

        System.out.println("\n===========================================================");
        System.out.println("                  Ulam Pattern Explorer");
        System.out.println("===========================================================");
        System.out.printf("%-45s | %3s | %3s%n", "Combination", "Len", "UlamDist");
        System.out.println("-----------------------------------------------------------");

        // 1. Baseline
        printAndSave(out, "A vs A'", A, Aprime);
        printAndSave(out, "AR vs A'R", AR, AprimeR);

        // 2. Interleaving
        printAndSave(out, "Interleave(A, A') vs Interleave(A', A)", interleave(A, Aprime), interleave(Aprime, A));
        printAndSave(out, "Interleave(A, A') vs A|A'", interleave(A, Aprime), concat(A, Aprime));

        // 3. Nested
        int[] nested1 = interleave(A, interleave(Aprime, reverse(A)));
        int[] nested2 = interleave(interleave(A, Aprime), reverse(Aprime));
        printAndSave(out, "Nested interleave1 vs Nested interleave2", nested1, nested2);

        // 4. Rotated
        printAndSave(out, "Interleave(A, rotate(A',1)) vs Interleave(rotate(A',1),A)",
                     interleave(A, AprimeRot), interleave(AprimeRot, A));

        // 5. Mirror concat
        printAndSave(out, "A|reverse(A') vs reverse(A)|A'", concat(A, reverse(Aprime)), concat(reverse(A), Aprime));
        printAndSave(out, "A'|reverse(A) vs reverse(A')|A", concat(Aprime, reverse(A)), concat(reverse(Aprime), A));

        out.close();
        System.out.println("===========================================================");
        System.out.println("\nFull results with permutations saved to: ulam_results.txt");
    }
}
