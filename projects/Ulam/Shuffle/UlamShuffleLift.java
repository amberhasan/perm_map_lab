import java.util.*;

/**
 * Implements the multistage "block shuffle" construction from:
 * "Explicit Good Codes Approaching Distance 1 in Ulam Metric"
 *
 * Core:
 *  - n = q^ell
 *  - Positions are indexed by ell base-q digits.
 *  - At stage i (1..ell), we partition positions into blocks of size q by fixing all digits except digit i.
 *  - For each block, we apply a "ground permutation" sigma in D to the i-th digit.
 *    The choice of sigma for each block is given by a stage shuffler w^(i) of length n/q over alphabet [p].
 *
 * Also includes O(n log n) Ulam distance between permutations via LIS of inverse composition.
 */
public class UlamShuffleLift {

    // -----------------------------
    // Construction
    // -----------------------------

    /**
     * Build permutation of [0..n-1] using the multistage construction.
     *
     * @param q base
     * @param ell number of stages, n=q^ell
     * @param D ground permutations, D[c][x] = sigma_c(x), each of length q, values 0..q-1
     * @param shufflers shufflers[stage][blockIndex] = c in [0..p-1], stage indexed 0..ell-1
     *                 Each stage has length n/q blocks.
     * @return permutation pi as int[n], pi[pos] = value at position pos
     */
    public static int[] build(int q, int ell, int[][] D, int[][] shufflers) {
        int n = ipow(q, ell);
        int p = D.length;

        if (shufflers.length != ell)
            throw new IllegalArgumentException("Need exactly ell shufflers (one per stage).");

        for (int c = 0; c < p; c++) {
            if (D[c].length != q) throw new IllegalArgumentException("Each ground permutation must have length q.");
        }

        // start with identity permutation pi^(0)[pos] = pos
        int[] pi = new int[n];
        for (int i = 0; i < n; i++) pi[i] = i;

        // Precompute base-q digits for each position: digits[pos][0..ell-1]
        // We'll use digit index 0..ell-1 (stage i corresponds to digit i, but paper uses 1..ell).
        int[][] digits = new int[n][ell];
        for (int pos = 0; pos < n; pos++) {
            int x = pos;
            for (int d = ell - 1; d >= 0; d--) { // most-significant at 0, least at ell-1 (consistent)
                digits[pos][d] = x % q;
                x /= q;
            }
        }

        // For each stage i, we apply within each block that varies digit i
        for (int stage = 0; stage < ell; stage++) {
            int blocks = n / q;
            if (shufflers[stage].length != blocks)
                throw new IllegalArgumentException("Stage " + stage + " shuffler must have length n/q = " + blocks);

            int[] next = new int[n];

            // Canonical mapping from (alpha,beta) to blockIndex:
            // Here we take the tuple of all digits except the stage digit, in lex order, to index blocks.
            // blockIndex = rank of the (ell-1)-tuple.
            //
            // For each position, compute its blockIndex by removing digit 'stage'.
            // Then within that block, the stage digit x in [0..q-1] is mapped to y = sigma_c[x].
            // next[pos(alpha,x,beta)] = pi[pos(alpha,y,beta)].
            for (int pos = 0; pos < n; pos++) {
                int blockIndex = blockIndexExcludingDigit(digits[pos], q, stage);
                int c = shufflers[stage][blockIndex];
                if (c < 0 || c >= p) throw new IllegalArgumentException("Invalid ground perm index in shuffler.");

                int x = digits[pos][stage];
                int y = D[c][x];

                // compute source position: same digits except stage digit replaced by y
                int srcPos = replaceDigitAndPack(digits[pos], q, stage, y);
                next[pos] = pi[srcPos];
            }

            pi = next;
        }

        return pi;
    }

    /**
     * Computes canonical block index by taking all digits except excludedDigit
     * as an (ell-1)-digit base-q number.
     */
    private static int blockIndexExcludingDigit(int[] dig, int q, int excludedDigit) {
        int idx = 0;
        for (int i = 0; i < dig.length; i++) {
            if (i == excludedDigit) continue;
            idx = idx * q + dig[i];
        }
        return idx;
    }

    /**
     * Packs digits into position index, replacing digit 'posDigit' with newVal.
     */
    private static int replaceDigitAndPack(int[] dig, int q, int posDigit, int newVal) {
        int idx = 0;
        for (int i = 0; i < dig.length; i++) {
            int v = (i == posDigit) ? newVal : dig[i];
            idx = idx * q + v;
        }
        return idx;
    }

    private static int ipow(int a, int e) {
        int r = 1;
        for (int i = 0; i < e; i++) r *= a;
        return r;
    }

    // -----------------------------
    // Fast Ulam distance for permutations
    // -----------------------------

    /**
     * Ulam distance for permutations pi, pj in S_n:
     * d_U = n - LCS(pi, pj)
     * For permutations, LCS(pi, pj) = LIS( inv(pi) composed with pj ) in one-line notation.
     */
    public static int ulamDistance(int[] pi, int[] pj) {
        int n = pi.length;
        if (pj.length != n) throw new IllegalArgumentException("Permutations must have same length.");

        int[] inv = new int[n];
        for (int pos = 0; pos < n; pos++) inv[pi[pos]] = pos;

        int[] seq = new int[n];
        for (int pos = 0; pos < n; pos++) seq[pos] = inv[pj[pos]];

        int lis = lisLength(seq);
        return n - lis;
    }

    /**
     * LIS length in O(n log n) for int array.
     */
    private static int lisLength(int[] a) {
        int[] tails = new int[a.length];
        int size = 0;
        for (int x : a) {
            int i = Arrays.binarySearch(tails, 0, size, x);
            if (i < 0) i = -i - 1;
            tails[i] = x;
            if (i == size) size++;
        }
        return size;
    }

    // -----------------------------
    // Demo / Example
    // -----------------------------
    public static void main(String[] args) {
        // Example matching the paper's Figure-2 style small demo:
        // q=3, ell=2 => n=9
        int q = 3, ell = 2;
        int n = ipow(q, ell);

        // Ground set D of size p=4 (each is a permutation of [0,1,2])
        // Here written as arrays sigma[x] = y
        int[][] D = {
                {0,1,2}, // 012
                {2,1,0}, // 210
                {1,0,2}, // 102
                {1,2,0}  // 120
        };

        // shufflers: ell stages, each length n/q = 3
        // Stage 1 shuffler w^(1) = 3 0 1   (over alphabet [0..3])
        // Stage 2 shuffler w^(2) = 2 2 3
        int[][] shufflers = {
                {3,0,1},
                {2,2,3}
        };

        int[] pi = build(q, ell, D, shufflers);
        System.out.println("n=" + n);
        System.out.println("Constructed permutation pi:");
        System.out.println(Arrays.toString(pi));

        // Compare with a second codeword (change shuffler a bit)
        int[][] shufflers2 = {
                {3,1,1},
                {2,0,3}
        };
        int[] pj = build(q, ell, D, shufflers2);

        int du = ulamDistance(pi, pj);
        System.out.println("\nSecond permutation pj:");
        System.out.println(Arrays.toString(pj));
        System.out.println("\nUlam distance dU(pi,pj) = " + du);
    }
}
