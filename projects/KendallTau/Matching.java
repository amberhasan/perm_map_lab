import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Matching {

    public static void main(String[] args) {
        List<Pair> pairs = findGrayCodePermutations(4);
        int i = 1;
        for (Pair pair : pairs) {
            System.out.println(i + " " + pair);
            i++;
        }
    }

    public static List<Pair> findGrayCodePermutations(int n) {
        List<int[]> permutations = generatePermutations(n);
        Collections.shuffle(permutations);

        List<Pair> pairs = new ArrayList<>();
        while (!permutations.isEmpty()) {
            int[] a = permutations.remove(0);
            for (int i = 0; i < permutations.size(); i++) {
                int[] b = permutations.get(i);
                if (isAdjacentTransposition(a, b)) {
                    pairs.add(new Pair(a, b));
                    permutations.remove(i);
                    break;
                }
            }
        }
        return pairs;
    }

    private static boolean isAdjacentTransposition(int[] a, int[] b) {
        int diffCount = 0;
        int lastDiff = -1;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                if (lastDiff != -1 && i - lastDiff != 1) {
                    return false;
                }
                diffCount++;
                lastDiff = i;
            }
        }
        return diffCount == 2;
    }

    private static List<int[]> generatePermutations(int n) {
        List<int[]> permutations = new ArrayList<>();
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i + 1;
        }
        permute(arr, 0, permutations);
        return permutations;
    }

    private static void permute(int[] arr, int k, List<int[]> permutations) {
        if (k == arr.length) {
            permutations.add(arr.clone());
        } else {
            for (int i = k; i < arr.length; i++) {
                swap(arr, i, k);
                permute(arr, k + 1, permutations);
                swap(arr, k, i);
            }
        }
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    static class Pair {
        int[] a;
        int[] b;

        Pair(int[] a, int[] b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return arrayToString(a) + " - " + arrayToString(b);
        }

        private String arrayToString(int[] array) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < array.length; i++) {
                sb.append(array[i]);
                if (i < array.length - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
