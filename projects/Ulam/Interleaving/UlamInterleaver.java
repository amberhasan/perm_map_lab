import java.util.*;

public class UlamInterleaver {
    static int s = 3; // length of each class
    static int t = 1; // number of translocations we want to correct
    static int n = s * (2*t + 1);
    static List<List<Integer>> classes = new ArrayList<>();

    public static void main(String[] args) {
        partitionClasses();
        System.out.println("Partitioned classes:");
        for (int i = 0; i < classes.size(); i++) {
            System.out.println("P" + (i+1) + " = " + classes.get(i));
        }

        // Build simple "codewords" for each class (all permutations for now)
        List<List<List<Integer>>> codebooks = new ArrayList<>();
        for (List<Integer> cls : classes) {
            codebooks.add(generateAllPermutations(cls));
        }

        // Build interleaved codewords (Cartesian product)
        List<List<Integer>> interleavedCode = buildInterleavedCode(codebooks);
        System.out.println("\nGenerated Interleaved Code:");
        for (List<Integer> cw : interleavedCode) {
            System.out.println(cw);
        }
    }

    static void partitionClasses() {
        for (int i = 0; i < (2*t+1); i++) classes.add(new ArrayList<>());
        for (int i = 1; i <= n; i++) {
            int idx = (i-1) % (2*t+1);
            classes.get(idx).add(i);
        }
    }

    static List<List<Integer>> generateAllPermutations(List<Integer> elements) {
        List<List<Integer>> result = new ArrayList<>();
        permuteHelper(elements, 0, result);
        return result;
    }

    static void permuteHelper(List<Integer> arr, int l, List<List<Integer>> result) {
        if (l == arr.size()) {
            result.add(new ArrayList<>(arr));
        } else {
            for (int i = l; i < arr.size(); i++) {
                Collections.swap(arr, i, l);
                permuteHelper(arr, l+1, result);
                Collections.swap(arr, i, l);
            }
        }
    }

    static List<List<Integer>> buildInterleavedCode(List<List<List<Integer>>> codebooks) {
        List<List<Integer>> results = new ArrayList<>();
        buildInterleavedRecursive(codebooks, 0, new ArrayList<>(), results);
        return results;
    }

    static void buildInterleavedRecursive(List<List<List<Integer>>> codebooks,
                                          int depth,
                                          List<List<Integer>> current,
                                          List<List<Integer>> results) {
        if (depth == codebooks.size()) {
            results.add(mergeClasses(current));
            return;
        }
        for (List<Integer> choice : codebooks.get(depth)) {
            current.add(choice);
            buildInterleavedRecursive(codebooks, depth+1, current, results);
            current.remove(current.size()-1);
        }
    }

    static List<Integer> mergeClasses(List<List<Integer>> chosenPerms) {
        List<Integer> merged = new ArrayList<>(n);
        for (int i = 0; i < n; i++) merged.add(0); // fill with zeros first

        int numClasses = chosenPerms.size();
        for (int classIndex = 0; classIndex < numClasses; classIndex++) {
            List<Integer> perm = chosenPerms.get(classIndex);
            int pos = 0;
            for (int i = 0; i < n; i++) {
                if (i % numClasses == classIndex) {
                    merged.set(i, perm.get(pos++));
                }
            }
        }
        return merged;
    }
}
