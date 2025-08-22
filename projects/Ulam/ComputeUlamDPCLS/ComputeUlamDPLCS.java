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
        // Example permutations
        String pi = "3124";
        String sigma = "1342";

        int lcsLength = longestCommonSubsequence(pi, sigma);
        int ulamDistance = pi.length() - lcsLength;

        System.out.println("pi = " + pi);
        System.out.println("sigma = " + sigma);
        System.out.println("LCS length = " + lcsLength);
        System.out.println("Ulam distance = " + ulamDistance);
    }
}
