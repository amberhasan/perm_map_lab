import java.util.*;
import java.io.*;

public class GreedySJT_LIS {

    static int n;
    static int d;

    // Store codewords and their inverses
    static List<int[]> code = new ArrayList<>();
    static List<int[]> inverses = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java GreedySJT_LIS <n> <d>");
            return;
        }

        n = Integer.parseInt(args[0]);
        d = Integer.parseInt(args[1]);

        SJTGenerator gen = new SJTGenerator(n);
        int[] p;
        long checked = 0;

        while ((p = gen.next()) != null) {
            checked++;

            boolean ok = true;
            int minDist = Integer.MAX_VALUE;

            for (int i = 0; i < code.size(); i++) {
                int dist = ulamDistanceAtLeast(p, inverses.get(i), d);
                if (dist < d) {
                    ok = false;
                    break;
                }
                minDist = Math.min(minDist, dist);
            }

            if (ok) {
                int[] pCopy = p.clone();
                code.add(pCopy);
                inverses.add(inverse(pCopy));
            }
        }

        System.out.println("\nFinal U(" + n + "," + d + ") size = " + code.size());
        saveCode();
    }

    /**
     * Ulam distance via LIS.
     * Returns >= d if possible, otherwise returns < d early.
     */
    static int ulamDistanceAtLeast(int[] p, int[] invQ, int threshold) {
        // Build sequence invQ[p[i]]
        int[] seq = new int[n];
        for (int i = 0; i < n; i++) {
            seq[i] = invQ[p[i]];
        }

        int lis = lisLengthAtMost(seq, n - threshold);
        return n - lis;
    }

    /**
     * LIS with early cutoff:
     * If LIS exceeds maxAllowed, we stop early.
     */
    static int lisLengthAtMost(int[] a, int maxAllowed) {
        int[] tails = new int[a.length];
        int size = 0;

        for (int x : a) {
            int i = Arrays.binarySearch(tails, 0, size, x);
            if (i < 0) i = -(i + 1);
            tails[i] = x;
            if (i == size) size++;

            // Early exit: distance would be < d
            if (size > maxAllowed) {
                return size;
            }
        }
        return size;
    }

    static int[] inverse(int[] p) {
        int[] inv = new int[p.length + 1];
        for (int i = 0; i < p.length; i++) {
            inv[p[i]] = i;
        }
        return inv;
    }

    static void saveCode() {
        String filename = "U" + n + "_" + d + "_GreedySJT_LIS.txt";
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("# U(" + n + "," + d + ") via Greedy SJT + LIS");
            out.println("# size = " + code.size());
            for (int[] p : code) {
                for (int i = 0; i < p.length; i++) {
                    out.print(p[i]);
                    if (i + 1 < p.length) out.print(" ");
                }
                out.println();
            }
            System.out.println("Saved code to " + filename);
        } catch (IOException e) {
            System.err.println("Write error: " + e.getMessage());
        }
    }
}

class SJTGenerator {
    int n;
    int[] perm;
    int[] dir;
    boolean first = true;

    public SJTGenerator(int n) {
        this.n = n;
        perm = new int[n];
        dir = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i + 1;
            dir[i] = -1;
        }
    }

    public int[] next() {
        if (first) {
            first = false;
            return perm;
        }

        int k = -1, kIdx = -1;

        for (int i = 0; i < n; i++) {
            int j = i + dir[i];
            if (j >= 0 && j < n && perm[i] > perm[j]) {
                if (k == -1 || perm[i] > k) {
                    k = perm[i];
                    kIdx = i;
                }
            }
        }

        if (k == -1) return null;

        int swapIdx = kIdx + dir[kIdx];
        swap(perm, kIdx, swapIdx);
        swap(dir, kIdx, swapIdx);

        for (int i = 0; i < n; i++) {
            if (perm[i] > k) dir[i] *= -1;
        }

        return perm;
    }

    private void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
