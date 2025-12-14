import java.io.*;
import java.util.*;

/**
 * Build length-12 permutations from length-6 permutations using
 * binary interleaving patterns.
 *
 * 0 -> take next from A
 * 1 -> take next from B (A + 6)
 */
public class InterleaveAB {

    static class Pattern {
        String bits;
        Pattern(String bits) { this.bits = bits; }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println(
                "Usage: java InterleaveAB perms6.txt patterns.txt output.txt"
            );
            return;
        }

        List<int[]> perms6 = readPerms(args[0]);
        List<Pattern> patterns = readPatterns(args[1]);

        try (PrintWriter out = new PrintWriter(new FileWriter(args[2]))) {
            int pid = 1;
            for (Pattern pat : patterns) {
                out.println("/* PATTERN " + pid + ": " + pat.bits + " */");

                List<int[]> built = new ArrayList<>();

                for (int[] p : perms6) {
                    int[] q = interleave(p, pat.bits);
                    built.add(q);
                    printPerm(out, q);
                }

                out.println();
                out.println("/* REVERSE */");
                for (int[] q : built) {
                    int[] r = reverse(q);
                    printPerm(out, r);
                }

                out.println();
                pid++;
            }
        }
    }

    /* ---------- core logic ---------- */

    static int[] interleave(int[] p, String bits) {
        int n = p.length;
        if (bits.length() != 2 * n)
            throw new IllegalArgumentException("Pattern length mismatch");

        int[] A = p;
        int[] B = new int[n];
        for (int i = 0; i < n; i++) B[i] = p[i] + n;

        int ia = 0, ib = 0;
        int[] out = new int[2 * n];

        for (int i = 0; i < bits.length(); i++) {
            char c = bits.charAt(i);
            if (c == '0') out[i] = A[ia++];
            else          out[i] = B[ib++];
        }
        return out;
    }

    static int[] reverse(int[] p) {
        int[] r = new int[p.length];
        for (int i = 0; i < p.length; i++)
            r[i] = p[p.length - 1 - i];
        return r;
    }

    /* ---------- I/O ---------- */

    static List<int[]> readPerms(String file) throws IOException {
        List<int[]> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] s = line.split("\\s+");
                int[] p = new int[s.length];
                for (int i = 0; i < s.length; i++)
                    p[i] = Integer.parseInt(s[i]);
                list.add(p);
            }
        }
        return list;
    }

    static List<Pattern> readPatterns(String file) throws IOException {
        List<Pattern> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty())
                    list.add(new Pattern(line));
            }
        }
        return list;
    }

    static void printPerm(PrintWriter out, int[] p) {
        for (int i = 0; i < p.length; i++) {
            if (i > 0) out.print(" ");
            out.print(p[i]);
        }
        out.println();
    }
}
