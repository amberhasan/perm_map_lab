import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class MainSJT {
    static int n, m, d, u;
    static int N, Nmax = 0;
    static int sz;
    static int[][] P;
    static String indexFile;
    static boolean indexSearch;

    // Global variables for SJT
    static int[] direction;
    static int[] directionM;
    static int[] sjtPermutation;
    static int[] sjtPermutationM;
    static int[] identityPermutation; //for indexPA
    static int[] identityPermutationM; //for fixedPA
    //TODO: rename the normal to indexPA and M to fixedPA
    static int numCalls = 0;

    public static void main(String[] args) {
        parseArgs(args);
        if(indexSearch)
            IndexSearch();
        else
            FixedSearch();
    }

    public static void initializeSJTSetup(){
        identityPermutation = createIdentityPermutation(n);
        sjtPermutation = identityPermutation;

        // Initialize the direction array
        direction = new int[n];
        Arrays.fill(direction, -1); // Assuming -1 for left, 1 for right
    }

    public static void initializeSJTSetupM(){
        identityPermutationM = createIdentityPermutation(m);
        sjtPermutationM = identityPermutationM;

        // Initialize the direction array
        directionM = new int[m];
        Arrays.fill(directionM, -1); // Assuming -1 for left, 1 for right
    }

    public static int[] createIdentityPermutation(int n) { //0-indexed
        int[] identity = new int[n];
        for (int i = 0; i < n; i++) {
            identity[i] = i;
        }
        return identity;
    }

    public static void IndexSearch() {

        System.out.println("Running Index Search");
        System.out.println("T(" + n + "," + m + "," + d + ")");

        while(true) {
            indexPA();
//            System.out.println("Nmax is " + Nmax);
//            System.out.println("N is " + N);
            if(Nmax < N) {
                Nmax = N;
                System.out.println("T(" + n + "," + m + "," + d + ")>=" + N);
                try {
                    File file = new File("T(" + n + "," + m + "," + d + ")-" + N + ".txt");
                    file.createNewFile();
                    FileWriter fp = new FileWriter(file);
                    for(int j = 0; j < N; j++) {
                        StringBuilder toWrite = new StringBuilder();
                        for(int i = 0; i < n - 1; i++) {
                            toWrite.append(P[j][i] + 1).append(" ");
                        }
                        toWrite.append(P[j][n - 1] + 1).append("\r\n");
                        fp.write(toWrite.toString());
                    }
                    fp.flush();
                    fp.close();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }

    public static void FixedSearch() {
        System.out.println("Running Fixed Search");
        ArrayList<int[]> indices = new ArrayList<>();
        HashMap<String, Integer> results = new HashMap<>();

        // read indexFile to obtain list of fixed indices
        try {
            BufferedReader file = new BufferedReader(new FileReader(indexFile));
            String line;
            while((line = file.readLine()) != null) {
                String[] perm = line.split(" ");
                ArrayList<Integer> idx = new ArrayList<>();
                for(int i=0; i<perm.length; i++) {
                    if(!perm[i].equals("0")) {
                        idx.add(i);
                    }
                }
                int[] arr = idx.stream().mapToInt(i -> i).toArray();
                indices.add(arr);
            }
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }

        int searchCount = 1;
        int totalCount = 0;

        // Step 1: Initialize ExecutorService
//        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for(int[] idxs : indices) {
            // printed indices are from [1,n], actual indices are [0, n-1]
            int[] printIdxs = idxs.clone();
            for(int i=0; i<printIdxs.length; i++)
                printIdxs[i]++;

            if(results.containsKey(Arrays.toString(idxs))) {
                System.out.println("(" + searchCount + "/" + indices.size() + ") A(" + n + "," + d + ")-" + Arrays.toString(printIdxs) + " already calculated");
                System.out.println("A(" + n + "," + d + ")-" + Arrays.toString(printIdxs) + " >= " + results.get(Arrays.toString(idxs)) + "\n");
                totalCount += results.get(Arrays.toString(idxs));
                searchCount++;
            }
            else if(results.containsKey(Arrays.toString(mirror(idxs)))) {
                int[] printMirror = mirror(idxs).clone();
                for(int i=0; i<printMirror.length; i++)
                    printMirror[i]++;
                System.out.println("(" + searchCount + "/" + indices.size() + ") A(" + n + "," + d + ")-" + Arrays.toString(printIdxs) + " is mirror of " + Arrays.toString(printMirror));
                System.out.println("A(" + n + "," + d + ")-" + Arrays.toString(printIdxs) + " >= " + results.get(Arrays.toString(mirror(idxs))) + "\n");
                totalCount += results.get(Arrays.toString(mirror(idxs)));
                searchCount++;
            }
            else {
                System.out.println("(" + searchCount + "/" + indices.size() + ") A(" + n + "," + d + ")-" + Arrays.toString(printIdxs));
                fixedPA(idxs);
                System.out.println("A(" + n + "," + d + ")-" + Arrays.toString(printIdxs) + " >= " + N + "\n");
                results.put(Arrays.toString(idxs), N);
                totalCount += N;
                searchCount++;
                try {
                    File file = new File("A(" + n + "," + d + ")-" + Arrays.toString(printIdxs)+ "-" + N + ".txt");
                    file.createNewFile();
                    FileWriter fp = new FileWriter(file);
                    for(int j = 0; j < N; j++) {
                        StringBuilder toWrite = new StringBuilder();
                        for(int i = 0; i < n - 1; i++) {
                            toWrite.append(P[j][i] + 1).append(" ");
                        }
                        toWrite.append(P[j][n - 1] + 1).append("\r\n");
                        fp.write(toWrite.toString());
                    }
                    fp.flush();
                    fp.close();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
        System.out.println("P(" + n + "," + d + ") >= " + totalCount);
    }

    public static void indexPA() {
        initializeSJTSetup();
        int[] data = new int[n];
        sz = 100_000;
        P = new int[sz][n];
        Random rand = new Random(System.currentTimeMillis());
        N = 0;
        // try and add u random permutations
        for(int j = 0; j < u; j++) {
            Arrays.fill(data, -1);
            for(int i = n - m; i < n; i++) {
                data[i] = i;
            }
            for(int i = 0; i < n; i++) {
                int irand = rand.nextInt(n - 1);
                swap(data, i, irand);
            }
            if(dist(data)) {
                addToPA(data);
            }
        }
        // create initial permutation
        Arrays.fill(data, -1);

        //so the last m digits are fixed, all before are -1
        for(int i = n - m; i < n; i++) {
            data[i] = i;
        }
        boolean hasNext = true;
        // iterate through all permutations

        while(hasNext) {
            if(dist(data)) {
                addToPA(data);
            }
            hasNext = findNextSJTPermutation();
            data = sjtPermutation;
            if (m < n) {
                for (int i = 0; i < n; i++) {
                    // Check if the value of data[i] is not among the last m values
                    if (data[i] < n - m) {
                        data[i] = -1;
                    }
                    // Else, keep the value as is since it's among the last m values
                }
            }
        }
    }

    public static void fixedPA(int[] idxs) {
        initializeSJTSetupM();
        int[] data = new int[m]; //this data is different, it's only size m
        sz = 100_000;
        P = new int[sz][n];
        Random rand = new Random(System.currentTimeMillis());
        N = 0;
        // try and add u random permutations
        for(int j = 0; j < u; j++) {
            for(int i = 0; i < m; i++) {
                data[i] = i;
            }
            for(int i = 0; i < m; i++) {
                int irand = rand.nextInt(m - 1);
                swap(data, i, irand);
            }
            // insert fixed indices
            int[] perm = insert(data, idxs);
            if(dist(perm)) {
                addToPA(perm);
            }
        }
        // create initial permutation
        for(int i = 0; i < m; i++) {
            data[i] = i;
        }
        boolean hasNext = true;
        // iterate through all permutations
        while(hasNext) {
            int[] perm = insert(data, idxs);
            if(dist(perm)) {
                addToPA(perm);
            }
            hasNext = findNextSJTPermutationM();
            if(sjtPermutationM == null){
                initializeSJTSetupM();
            }
            data = sjtPermutationM;
        }
//        System.out.println("Adding permutation to PA: " + Arrays.toString(data));
    }

    public static void swap(int[] data, int left, int right) {
        int temp = data[left];
        data[left] = data[right];
        data[right] = temp;
    }

    public static void reverse(int[] data, int left, int right) {
        while(left < right) {
            int temp = data[left];
            data[left++] = data[right];
            data[right--] = temp;
        }
    }

    public static int[] insert(int[] data, int[] indices) {
        int[] perm = new int[data.length + indices.length];
        for(int i=0; i<indices.length; i++)
            perm[indices[i]] = -1;
        int curIndex = 0;
        for(int i=0; i<data.length; i++) {
            while(perm[curIndex] == -1)
                curIndex++;
            perm[curIndex] = data[i];
            curIndex++;
        }
        return perm;
    }

    public static int[] mirror(int[] idxs) {
        int[] midxs = new int[idxs.length];
        for(int i=0; i<idxs.length; i++)
            midxs[i] = n - 1 - idxs[i];
        Arrays.sort(midxs);
        return midxs;
    }

    public static boolean dist(int[] data) {
        for(int j = 0; j < N; j++) {
            if(!kendallDistance(data, P[j])) {
                return false;
            }
        }
        return true;
    }

    public static boolean kendallDistance(int[] p1, int[] p2) {
        int[] p2_copy = new int[n];
        System.arraycopy(p2, 0, p2_copy, 0, n);

        int res = 0;
        for(int i = 0; i < n; i++) {
            int k = i;

            while(p1[i] != p2_copy[k]) {
                k++;
            }
            for(int j = k - 1; j >= i; j--) {
                swap(p2_copy, j, j + 1);
                res++;
                if(res >= d) {
                    return true;
                }
            }
        }
//        System.out.println("Kendall tau distance between " + Arrays.toString(p1) + " and " + Arrays.toString(p2) + " is: " + res);
        return false;
    }

    private static void addToPA(int[] data) {
        System.arraycopy(data, 0, P[N], 0, n);
        N++;
        if(N == sz) {
            int[][] tmpP = new int[2 * sz][n];
            System.arraycopy(P, 0, tmpP, 0, sz);
            sz = 2 * sz;
            P = tmpP;
        }
    }

    public static boolean findNextSJTPermutation() {
        numCalls++;
        if(numCalls == 1){
            sjtPermutation = identityPermutation;
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

    public static boolean findNextSJTPermutationM() {
        numCalls++;
        if(numCalls == 1){
            sjtPermutationM = identityPermutation;
            return true;
        }
        // Find the largest mobile element k
        int k = -1, kIndex = -1;
        for (int i = 0; i < sjtPermutationM.length; i++) {
            int adjacentNumIndex = i + directionM[i];
            if (adjacentNumIndex >= 0 && adjacentNumIndex < sjtPermutationM.length) {
                if ((directionM[i] == -1 && sjtPermutationM[i] > sjtPermutationM[adjacentNumIndex]) ||
                        (directionM[i] == 1 && sjtPermutationM[i] > sjtPermutationM[adjacentNumIndex])) {
                    if (k == -1 || sjtPermutationM[i] > sjtPermutationM[kIndex]) {
                        k = sjtPermutationM[i];
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
        int swapIndex = kIndex + directionM[kIndex];
        swap(sjtPermutationM, kIndex, swapIndex);
        swap(directionM, kIndex, swapIndex);

        // Reverse the direction of all elements larger than k
        for (int i = 0; i < sjtPermutationM.length; i++) {
            if (sjtPermutationM[i] > k) {
                directionM[i] = -directionM[i];
            }
        }
        return true;
    }

    public static void parseArgs(String[] args) {
        if(args.length == 4) {
            n = Integer.parseInt(args[0]);
            m = Integer.parseInt(args[1]);
            d = Integer.parseInt(args[2]);
            u = Integer.parseInt(args[3]);
            indexSearch = true;
        }
        else if(args.length == 3) {
            n = 5;
            m = 5;
            d = Integer.parseInt(args[0]);
            u = Integer.parseInt(args[1]);
            indexFile = args[2];
            indexSearch = false;
            try {
                BufferedReader file = new BufferedReader(new FileReader(indexFile));
                String[] perm = file.readLine().split(" ");
                n = perm.length;
                m = 0;
                for(int i=0; i<n; i++) {
                    if(perm[i].equals("0"))
                        m++;
                }
            } catch (Exception e) {
                System.out.println(e);
                System.exit(0);
            }
        }
        else {
            System.out.println("invalid parameters\n");
            System.out.println("for index search:");
            System.out.println("   java KendallMultiSearch <n> <m> <d> <u>\n");
            System.out.println("for fixed search:");
            System.out.println("   java KendallMultiSearch <d> <u> <indexFileName>");
            System.exit(0);
        }
    }

    private static void printPermutation(int[] permutation) {
        for (int i : permutation) {
            System.out.print(i + " ");
        }
        System.out.println();
    }

}