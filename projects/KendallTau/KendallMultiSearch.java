import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class KendallMultiSearch {
    
    static int n, m, d, u;
    static int N, Nmax = 0;
    static int sz;
    static int[][] P;
    static String indexFile;
    static boolean indexSearch;

    public static void main(String[] args) {
        args = new String[] {"9", "5", "2", "1"};
//        args = new String[] {String.valueOf(3), String.valueOf(1), "T(5,4,3)-20.txt"};
//        args = new String[] {"4", "2", "2", "1"};
//        args = new String[] {"5", "3", "2", "1"};

//        args = new String[] {"2", "1", "T(5,3,2)-30.txt"};
//        args = new String[] {"2", "1", "T(9,5,2)-7560.txt"};
//        args = new String[] {"2", "1", "T(5,3,2)-30.txt"};

        parseArgs(args);

        if(indexSearch)
            IndexSearch();
        else
            FixedSearch();
    }
    
    public static void IndexSearch() {
        System.out.println("Running Index Search");
        System.out.println("T(" + n + "," + m + "," + d + ")");
        while(true) {
            indexPA();
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
        for(int i = n - m; i < n; i++) {
            data[i] = i;
        }
        boolean hasNext = true;
        // iterate through all permutations
        while(hasNext) {
            if(dist(data)) {
                addToPA(data);
            }
            hasNext = findNextPermutation(data);
        }
    }

    public static void fixedPA(int[] idxs) {
        int[] data = new int[m];
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
            System.out.println("Here's the data i wanna find the next perm for" + Arrays.toString(data));
            hasNext = findNextPermutation(data);
        }
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
        System.out.println("Here is the data" + Arrays.toString(data));
        System.out.println("Here is the indices" + Arrays.toString(indices));
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
    
    public static boolean findNextPermutation(int[] data) {
        int length = n;
        if(!indexSearch)
            length = m;
        int last = length - 2;
        // find the longest non-increasing suffix and pivot
        while (last >= 0) {
            if (data[last] < data[last + 1]) {
                break;
            }
            last--;
        }
        // If there is no increasing pair, there is no higher order permutation
        if(last < 0) {
            return false;
        }
        int nextGreater = length - 1;
        // Find the rightmost successor to the pivot
        for(int i = nextGreater; i > last; i--) {
            if (data[i] > data[last]) {
                nextGreater = i;
                break;
            }
        }
        // Swap the successor and the pivot
        swap(data, nextGreater, last);
        // Reverse the suffix
        reverse(data, last + 1, length - 1);
        // Return true as the next_permutation is done
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
}
