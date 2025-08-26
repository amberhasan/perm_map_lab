public class ComputeUlamDPLCS {

    // Standard LCS DP
    public static int longestCommonSubsequence(String text1, String text2) {
        int[][] lcs = new int[text1.length() + 1][text2.length() + 1];

        for (int i = text1.length() - 1; i >= 0; i--) {
            for (int j = text2.length() - 1; j >= 0; j--) {
                if (text1.charAt(i) == text2.charAt(j)) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        return lcs[0][0];
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ComputeUlamDPLCS <perm1> <perm2> ...");
            System.out.println("Example: java ComputeUlamDPLCS 3124 1342 4321");
            return;
        }

        int n = args[0].length();  // assume all permutations same length
        int minDistance = Integer.MAX_VALUE;

        // Compute all pairwise Ulam distances
        for (int i = 0; i < args.length; i++) {
            for (int j = i + 1; j < args.length; j++) {
                String pi = args[i];
                String sigma = args[j];

                int lcsLength = longestCommonSubsequence(pi, sigma);
                int ulamDistance = pi.length() - lcsLength;

                if (ulamDistance < minDistance) {
                    minDistance = ulamDistance;
                }
            }
        }
        //input can be: java ComputeUlamDPLCS 3124 1342 4321
        System.out.println("Minimum Ulam distance of this set = " + minDistance);
        System.out.println("This is a PA(" + n + "," + minDistance + ")");
    }
}
