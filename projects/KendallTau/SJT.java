import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

//prints out all SJT permutations.
public class SJT {
    static int n = 4;
    static int m = 2;
    static int d = 3;
    static int u = 1;
    static int N, Nmax = 0;
    static int sz;
    static int[][] P;
    static String indexFile;
    static boolean indexSearch;

    // Global variables for SJT
    static int[] direction;
    static int[] sjtPermutation;
    static int numCalls = 0;

    public static void main(String[] args) {

        // Initialize the direction array
        direction = new int[4];
        Arrays.fill(direction, -1); // Assuming -1 for left, 1 for right

        sjtPermutation = new int[]{ 1, 2, 3, 4 };
//        printPermutation(); // Print initial permutation

        // Generate and print next few permutations
        for (int i = 0; i < 24; i++) {
            if (findNextSJTPermutation()) {
                printPermutation();
            }
        }
        args = new String[] {String.valueOf(n), String.valueOf(m), String.valueOf(d), String.valueOf(u)};
//        args = new String[] {String.valueOf(d), String.valueOf(u), "T(14,2,11)-5.txt"};

//        parseArgs(args);
//
//        if(indexSearch)
//            IndexSearch();
//        else
//            FixedSearch();
    }

    public static void swap(int[] data, int left, int right) {
        int temp = data[left];
        data[left] = data[right];
        data[right] = temp;
    }

    public static boolean findNextSJTPermutation() {
        if(numCalls == 0){
            numCalls++;
            sjtPermutation = new int[]{1,2,3,4};
            return true;
        }
        // Find the largest mobile element k
        int k = -1, kIndex = -1;
        for (int i = 0; i < sjtPermutation.length; i++) {
            int adjacentNumIndex = i + direction[i];
            if (adjacentNumIndex >= 0 && adjacentNumIndex < sjtPermutation.length) {
                if ((direction[i] == -1 && sjtPermutation[i] > sjtPermutation[adjacentNumIndex]) ||
                        (direction[i] == 1 && sjtPermutation[i] > sjtPermutation[adjacentNumIndex])) {
                    if (k == -1 || sjtPermutation[i] > sjtPermutation[kIndex]) {
                        k = sjtPermutation[i];
                        kIndex = i;
                    }
                }
            }
        }

        // No mobile element means this was the last permutation
        if (k == -1) {
            return false;
        }

        // Swap k with the element in its direction
        int swapIndex = kIndex + direction[kIndex];
        swap(sjtPermutation, kIndex, swapIndex);
        swap(direction, kIndex, swapIndex);

        // Reverse the direction of all elements larger than k
        for (int i = 0; i < sjtPermutation.length; i++) {
            if (sjtPermutation[i] > k) {
                direction[i] = -direction[i];
            }
        }

        return true;
    }


    private static void printPermutation() {
        for (int i : sjtPermutation) {
            System.out.print(i + " ");
        }
        System.out.println();
    }

}