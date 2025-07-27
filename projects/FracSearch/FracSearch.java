import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FracSearch {
    // Parameter Variables
    static int prime;
    static int power;
    static int fdegree;
    static int gdegree;

    static List<Integer> fixedZeroDegrees;
    static List<Integer> fixedDegrees = new ArrayList<>();
    static List<Integer> fixedIndexes = new ArrayList<>();
    static List<boolean[]> masksToRemove = new ArrayList<>();

    static boolean verbose;
    // static boolean outputPerms;
    // Arithmetic Tables
    static int[][] subtractionTable;
    static int[][] divisionTable;
    static int[][] powerTable;
    // Program Variables
    static HashMap<Integer, ArrayList<Integer>> fFGMapValues; // maps index of f to array of minimum values needed by FG-Map
    static HashMap<Integer, ArrayList<Integer>> gFGMapValues; // maps index of g to array of minimum values needed by FG-Map
    static int[] lockIndex; // [0]: 0=f, 1=g; [1]: index of coefficient being fixed by FG-Map
    static int[] lockValues; // minimum values needed to cycle through lockIndex
    static int curLockValueIndex; // current value of lockIndex
    static ArrayList<boolean[]> fBitMasks; // bit masks for f
    static ArrayList<boolean[]> gBitMasks; // bit masks for g
    static boolean pDividesG;
    static long count;
    static long totalToCheck;
    static long startTime;

    public static void main(String[] args) throws IOException {
        parseArgs(args);
        //prime = 2;
        //power = 4;
        //fdegree = 3;
        //gdegree = 2;
        //GF.initGF(prime, power);
        //verbose = true;

        System.out.println(GF.irr+"\n");
        subtractionTable = fillSubtractionTable();
        divisionTable = fillDivisionTable();
        powerTable = fillPowerTable();
        fFGMapValues = getMinFGMapValues(fdegree);
        gFGMapValues = getMinFGMapValues(gdegree);
        fBitMasks = createFBitMasks();
        gBitMasks = createGBitMasks();
        pDividesG = gdegree % prime == 0;

        // Print FG-Map Values and Masks
        /*System.out.println("f(x) FG-Map Values");
        for(int key : fFGMapValues.keySet()) {
            System.out.println(key+": "+ fFGMapValues.get(key));
        }
        System.out.println("g(x) FG-Map Values");
        for(int key : gFGMapValues.keySet()) {
            System.out.println(key+": "+ gFGMapValues.get(key));
        }
        System.out.println("fBitMasks Masks: "+fBitMasks.size());
        for(boolean[] mask : fBitMasks) {
            System.out.println(Arrays.toString(mask));
        }
        System.out.println("gBitMasks Masks: "+gBitMasks.size());
        for(boolean[] mask : gBitMasks) {
            System.out.println(Arrays.toString(mask));
        }*/

        String outFileName = "frac_" + prime + "_" + power + "_" + fdegree + "_" + gdegree + ".txt";
        fixNumeratorDegreesToZero();

        StringBuffer totalFixedDegrees = new StringBuffer();
        for(int i=0; i<fixedZeroDegrees.size(); i++){
            totalFixedDegrees.append(fixedZeroDegrees.get(i) + "->0,");
        }
        for(int i=0; i<fixedDegrees.size(); i++){
            totalFixedDegrees.append(fdegree-fixedDegrees.get(i) + "->" + fixedIndexes.get(i) + ",");
        }
        System.out.println("Here is totalFixed Degrees " + totalFixedDegrees);
        BufferedWriter outFile = new BufferedWriter(new FileWriter(outFileName));
        outFile.write(GF.irr+"\r\n");
        outFile.flush();

        HashSet<String> foundFracPPs = new HashSet<>();
        count = 0;
        totalToCheck = totalToCheck();
        startTime = System.currentTimeMillis();
        Runnable updateConsole = () -> { update(); };
        ScheduledExecutorService updateThread = Executors.newScheduledThreadPool(1);
        updateThread.schedule(updateConsole, 10, TimeUnit.SECONDS);
        updateThread.schedule(updateConsole, 30, TimeUnit.SECONDS);
        updateThread.scheduleAtFixedRate(updateConsole, 1, 1, TimeUnit.MINUTES);
        // Main Loop
        for(boolean[] gmask : gBitMasks) {
            for(boolean[] fmask : fBitMasks) {
                //Find lock index
                lockIndex = getLockIndex(fmask, gmask);
                if(lockIndex[0] == 0) { // locking an index in f
                    lockValues = fFGMapValues.get(lockIndex[1]).stream().mapToInt(i->i).toArray();
                }
                else { // locking an index in g
                    lockValues = gFGMapValues.get(lockIndex[1]).stream().mapToInt(i->i).toArray();
                }
                curLockValueIndex = 1;  //skipping value 0 at index 0
                //Create initial pp
                int[] g = createGPolynomial(gmask);
                int[] gMaskIndexes = listIndexes(gmask);
                //System.out.println(Arrays.toString(fmask) + " / " + Arrays.toString(gmask) + " " + lockIndex[0] + " " + lockIndex[1]);
                do {
                    int[] gValues = evaluatePolynomial(g);
                    if(containsZero(gValues)) { // if g(x) produces a 0, skip it since we can't divide by zero
                        count += totalSkipped(fmask, gmask);
                        continue;
                    }
                    int[] f = createFPolynomial(fmask);
                    int[] fMaskIndexes = listIndexesF(fmask);
                    do {
                        count++;
                        if(checkPerm(f, gValues)
                                && !foundFracPPs.contains(Arrays.toString(f) + " / " + Arrays.toString(g))
                                && isOne(polyGCD(f, g))) {
                            HashMap<String, int[][]> equivalenceClass = getFGMaps(f, g);
                            if(pDividesG) {
                                equivalenceClass = getFofXPlusBMaps(equivalenceClass);
                            }
                            String toWrite = "";
                            for(String key : equivalenceClass.keySet())
                                toWrite += key + "\r\n";
                            outFile.write(toWrite);
                            outFile.flush();
                            foundFracPPs.addAll(equivalenceClass.keySet());
                            String output = Arrays.toString(f) + " / " + Arrays.toString(g);
                            while(output.length() < maxStringLength())
                                output += " ";
                            if(verbose) {
                                System.out.println(equivalenceClass.size() + " NFPPs Found. "+ foundFracPPs.size() + " Total.");
                                System.out.println(toWrite);
                            }
                            else {
                                System.out.println(output + "   " + equivalenceClass.size() + " NFPPs Found. "+ foundFracPPs.size() + " Total.");
                            }
                        }
                        //update();
                    }
                    while(incrementPolynomial(f, fMaskIndexes, 0));

                }
                while(incrementPolynomial(g, gMaskIndexes, 1));
            }
            fBitMasks.removeAll(masksToRemove);
        }
        outFile.close();
        updateThread.shutdownNow();
        long endTime = System.currentTimeMillis();
        DecimalFormat df = new DecimalFormat("##.##");
        System.out.println("\nSearch Complete");
        System.out.println(df.format((float)(endTime-startTime)/60000) + " min elapsed");
        System.out.println(foundFracPPs.size() + " NFPPs Found");
    }

    public static int add(int a, int b) { // a + b in GF
        return GF.addTable[a + b * GF.n];
    }

    public static int subtract(int a, int b) { // a - b in GF
        return subtractionTable[a][b];
    }

    public static int mult(int a, int b) { // a * b in GF
        return GF.mulTable[a + b * GF.n];
    }

    public static int divide(int a, int b) { // a / b in GF
        return divisionTable[a][b];
    }

    public static int power(int a, int b) { // a^b in GF
        return powerTable[a][b];
    }

    public static int[][] fillSubtractionTable() {
        int[][] subtraction = new int[GF.n][GF.n];
        for(int a = 0; a < GF.n; a++) {
            for(int b = 0; b < GF.n; b++) {
                for(int i=0; i<GF.n; i++) {
                    if(GF.addTable[b*GF.n + i] == a) {
                         subtraction[a][b] = i;
                    }
                }
            }
        }
        return subtraction;
    }

    public static int[][] fillDivisionTable() {
        int[][] division = new int[GF.n][GF.n];
        for(int a = 0; a < GF.n; a++) {
            division[a][0] = -1; // -1 as error for divide by zero
        }
        for(int a = 0; a < GF.n; a++) {
            for(int b = 1; b < GF.n; b++) {
                for(int i=0; i<GF.n; i++) {
                    if(GF.mulTable[b*GF.n + i] == a) {
                         division[a][b] = i;
                    }
                }
            }
        }
        return division;
    }

    public static int[][] fillPowerTable() {
        int maxDegree = Math.max(fdegree+1, gdegree+1);
        if(maxDegree <= prime+1)
            maxDegree = prime + 1;
        int[][] powers = new int [GF.n][maxDegree];
        for(int base = 0; base < GF.n; base++) {
            for(int pow = 0; pow < maxDegree; pow++) {
                powers[base][pow] = calcPower(base, pow);
            }
        }
        return powers;
    }

    public static int calcPower(int a, int b) { //a^b in GF
        if(b==0)
            return 1;
        if(b==1)
            return a;
        else
            return mult(a, calcPower(a,b-1));
    }

    public static int[] addPoly(int[] p1, int[] p2) {
        if(p1.length >= p2.length) {
            int[] newP = Arrays.copyOf(p1, p1.length);
            for(int i=0; i<p2.length; i++)
                newP[newP.length-1-i] = add(p1[p1.length-1-i], p2[p2.length-1-i]);
            return newP;
        }
        else {
            int[] newP = Arrays.copyOf(p2, p2.length);
            for(int i=0; i<p1.length; i++)
                newP[newP.length-1-i] = add(p1[p1.length-1-i], p2[p2.length-1-i]);
            return newP;
        }
    }

    public static int[] subtractPoly(int[] p1, int[] p2) { //subtract polynomials, p1 - p2
        // degree of p1 >= p2
        int[] newP = Arrays.copyOf(p1, p1.length);
        int degDiff = p1.length - p2.length;
        for(int i=degDiff; i<p1.length; i++)
            newP[i] = subtract(p1[i], p2[i - degDiff]);
        // trim leading zeros
        int i = 0;
        while(i < newP.length && newP[i] == 0) {
            i++;
        }
        if(i == 0)
            return newP;
        if(i == newP.length)
            return new int[] {0};
        return Arrays.copyOfRange(newP, i, newP.length);
    }

    public static int[] multA(int[] p, int a) { //multipy polynomial p by a
        int[] pa = Arrays.copyOf(p, p.length);
        for(int i=0; i<pa.length; i++)
            pa[i]=mult(p[i],a);
        return pa;
    }

    public static int[] multX(int[] p) { //multiply polynomial p by x
        int[] px = new int[p.length+1];
        px[px.length-1] = 0;
        System.arraycopy(p, 0, px, 0, p.length);
        return px;
    }

    public static int[] divideA(int[] p, int a) { //divide polynomial p by coefficient a
        int[] pa = Arrays.copyOf(p, p.length);
        for(int i=0; i<pa.length; i++)
            pa[i]=divide(p[i],a);
        return pa;
    }

    public static int[] multPoly(int[] p1, int[] p2) { // multiply polynomials p1 and p2
        int[] result = {0};
        for(int i=0; i<p1.length; i++) {
            if(p1[i] != 0) {
                int[] curPoly = Arrays.copyOf(p2, p2.length);
                int curDegree = p1.length-1-i;
                for(int j=0; j<curDegree; j++)
                    curPoly = multX(curPoly);
                curPoly = multA(curPoly, p1[i]);
                result = addPoly(result, curPoly);
            }
        }
        return result;
    }

    public static int[] getRemainder(int[] a, int[] b) { // returns remainder of polynomial a / polynomial b
        // Euclidean Division: https://en.wikipedia.org/wiki/Polynomial_greatest_common_divisor#Euclidean_division
        int[] r = Arrays.copyOf(a, a.length);
        int d = b.length - 1;
        if(d == 0) { // not polynomial division, no remainder
            return new int[] {0};
        }
        int c = b[0];
        while(r.length-1 >= d) {
            int[] s = new int[r.length - d];
            Arrays.fill(s, 0);
            s[0] = divide(r[0], c);
            int[] sb = multPoly(s, b);
            r = subtractPoly(r, sb);
        }
        return r;
    }

    public static int[] polyGCD(int[] a, int[] b) { // returns GCD of a and b
        //Euclid's Algorithm: https://en.wikipedia.org/wiki/Polynomial_greatest_common_divisor#Euclidean_division
        int[] rPrev = Arrays.copyOf(a, a.length);
        int[] rCur = Arrays.copyOf(b, b.length);
        while(rCur.length != 1 || rCur[0] != 0) {
            int[] rNext = getRemainder(rPrev, rCur);
            rPrev = rCur;
            rCur = rNext;
        }
        if(rPrev.length == 1) // if remaind is just a constant term
            return new int[] {1};
        return rPrev;
    }

    public static boolean isOne(int[] p) {
        return p.length == 1 && p[0] == 1;
    }

    public static int[] binomialToPower(int[] p1, int c, int n) { //p1 = (x+c), return (x+c)^n
        if(n == 1)
            return p1;
        else {
            int[] timesX = multX(p1);
            int[] timesB = multA(p1, c);
            int[] result = addPoly(timesX, timesB);
            return binomialToPower(result, c, n-1);
        }
    }

    public static int[] fOfXPlusB(int[] p, int b) { // returns f(x+b)
        int[] result = {p[p.length-1]}; //start with result as constant of p
        for(int i=p.length-2; i>=0; i--) {
            int[] binomial = {1, b}; //(x+b)
            int[] poly = binomialToPower(binomial, b, p.length-1-i); //(x+b)^p.length-1-i
            int[] timesCoef = multA(poly, p[i]); // a*(x+b)^p.length-1-i
            result = addPoly(timesCoef, result);
        }
        return result;
    }

    public static int evaluateX(int x, int[] polynomial) { // evaluate f(x)
        int degree = polynomial.length - 1;
        int sum = 0;
        for(int y=0; y<polynomial.length; y++) {
            if(polynomial[y] != 0) {
                int pow = power(x, degree-y); //raise x to degree of term y
                sum = add(sum, mult(pow, polynomial[y])); //multiply pow by coeff of term y, and increment sum by result
            }
        }
        return sum;
    }

    public static int[] evaluatePolynomial(int[] polynomial) { // evaluate f(x) for all x in GF
        int[] result = new int[GF.n];
        for(int x=0; x<GF.n; x++) {
            result[x] = evaluateX(x, polynomial);
        }
        return result;
    }

    public static boolean containsZero(int[] array) {
        for(int i=0; i< array.length; i++) {
            if(array[i] == 0)
                return true;
        }
        return false;
    }

    public static boolean checkPerm(int[] f, int[] gValues) { // check if f(x)/g(x) is a permutation fraction (pass g as values since we already calculated them)
        HashSet<Integer> values = new HashSet<>();
        for(int x=0; x<GF.n; x++) {
            int fValue = evaluateX(x, f);
            if(!values.add(divide(fValue, gValues[x]))) // add f(x)/g(x) to set, return false if it already exists (ie, not a perm)
                return false;
        }
        return true;
    }

    public static ArrayList<ArrayList<Integer>> getGOrbits() {
        ArrayList<ArrayList<Integer>> gOrbits = new ArrayList<>();
        ArrayList<Integer> field = new ArrayList<>();
        for(int i=0; i<GF.n; i++) {
            field.add(i);
        }
        for(int i=0; i<GF.n; i++) {
            if(field.contains(i)) {
                ArrayList<Integer> currentOrbit = new ArrayList<>();
                int currentElement = i;
                while(!currentOrbit.contains(currentElement)) {
                    currentOrbit.add(currentElement);
                    currentElement = power(currentElement, prime);
                }
                Collections.sort(currentOrbit);
                field.removeAll(currentOrbit);
                gOrbits.add(currentOrbit);
            }
        }
        return gOrbits;
    }

    public static HashMap<Integer, ArrayList<Integer>> getMinFGMapValues(int degree) {
        ArrayList<ArrayList<Integer>> gOrbits = getGOrbits();
        HashMap<Integer, ArrayList<Integer>> indexElements = new HashMap<>();
        for(int i=0; i<=degree; i++) {
            ArrayList<Integer> field = new ArrayList<>();
            for(int j=0; j<GF.n; j++) {
                field.add(j);
            }
            ArrayList<Integer> toAdd = new ArrayList<>();
            for(ArrayList<Integer> gOrbit : gOrbits) {
                HashSet<Integer> fullOrbit = new HashSet<>();
                if(field.contains(gOrbit.get(0))) {
                    for(int element : gOrbit) {
                        int currentElement = element;
                        while(fullOrbit.add(currentElement)) {
                            currentElement = mult(currentElement, i+1);
                        }
                    }
                    //System.out.println(i+": ");
                    //for(int k : fullOrbit)
                    //    System.out.println("   "+k);
                    field.removeAll(fullOrbit);
                    Integer[] fullOrbitArray = fullOrbit.toArray(new Integer[fullOrbit.size()]);
                    Arrays.sort(fullOrbitArray);
                    toAdd.add(fullOrbitArray[0]);
                }
            }
            indexElements.put(i, toAdd);
            //System.out.println("printing "+toAdd);
        }
        return indexElements;
    }

    public static boolean[] createMask(BitSet bs, int degree) {
        boolean[] mask = new boolean[degree + 1];
        Arrays.fill(mask, false);
        for(int i=0; i<bs.length(); i++) {
            if(bs.get(i)) {
                mask[degree-1-i] = true;
            }
        }
        return mask;
    }

    public static ArrayList<boolean[]> createFBitMasks() {
        ArrayList<boolean[]> masks = new ArrayList<>();
        if(fdegree == 1) {
            boolean[] mask = {false, false};
            masks.add(mask);
            return masks;
        }
        int numMasks = (int) Math.pow(2,fdegree-1); // eg. deg=5: [F X X X X F]
        for(int x=0; x<numMasks; x++) {
            long[] curVal = {x};
            BitSet bs = BitSet.valueOf(curVal); // creates binary representation of x
            boolean[] mask = createMask(bs, fdegree);
            masks.add(mask);
        }
        return masks;
    }

    public static ArrayList<boolean[]> createGBitMasks() {
        ArrayList<boolean[]> masks = new ArrayList<>();
        if(gdegree == 1) {
            boolean[] mask = {false, true};
            masks.add(mask);
            return masks;
        }
        int gapDegree = -1; // first index in gap for b-normalization
        if(gdegree % prime == 0 && prime == 2) {
            gapDegree = getGapDegree();
        }
        int numMasks = (int) Math.pow(2,gdegree-1); // eg. deg=5: [F F X X X F]
        for(int x=0; x<numMasks; x++) {
            long[] curVal = {x};
            BitSet bs = BitSet.valueOf(curVal); // creates binary representation of x

            // Case 1: p does not divide g
            if(gdegree % prime != 0) {
                if(bs.get(gdegree - 2) == false ) { // second coefficient fixed
                    boolean[] mask = createMask(bs, gdegree);
                    mask[mask.length-1] = true;
                    masks.add(mask);
                }
            }

            // Case 2: p divides g, p > 2
            else if(prime > 2) {
                if(bs.get(gdegree - 2) == false || bs.get(gdegree - 3) == false) { // m-normalization
                    boolean[] mask = createMask(bs, gdegree);
                    mask[mask.length-1] = true;
                    masks.add(mask);
                }
            }

            // Case 3: p divides g, p = 2
            else {
                if(gapDegree == -1) { // gedgree is 2 less than a power of 2, i.e. no gap
                    boolean[] mask = createMask(bs, gdegree);
                    mask[mask.length-1] = true;
                    masks.add(mask);
                }
                else { // use gapIndex for b-normalization
                    if(bs.get(gapDegree-1) == false || bs.get(gapDegree - 2) == false) { // b-normalization
                        boolean[] mask = createMask(bs, gdegree);
                        mask[mask.length-1] = true;
                        masks.add(mask);
                    }
                }
            }
        }
        return masks;
    }

    public static int getGapDegree() { // find start degree of gap defined by b-normalization
        if(((gdegree+2) & (gdegree+1)) == 0) // if fedgree is 2 less than a power of 2
            return -1; //no Gap
        int powerTwo = 1;
        while(2*powerTwo <= gdegree) { // while gdegree is <= the next power of 2
            powerTwo *= 2;
        }
        int gapDegree = powerTwo-1;
        return gapDegree;
    }

    public static int[] getLockIndex(boolean[] fmask, boolean[] gmask) {
        int smallestIndex = -1;
        int smallestSize = Integer.MAX_VALUE;
        int polynomial = -1; // 0 = f, 1 = g
        for(int x=0; x<=fdegree; x++) {
            if(fFGMapValues.get(x).size() < smallestSize && fmask[x]) {
                smallestIndex = x;
                smallestSize = fFGMapValues.get(x).size();
                polynomial = 0;
            }
        }
        for(int x=0; x<=gdegree; x++) {
            if(gFGMapValues.get(x).size() < smallestSize && gmask[x]) {
                smallestIndex = x;
                smallestSize = gFGMapValues.get(x).size();
                polynomial = 1;
            }
        }
        return new int[] {polynomial, smallestIndex};
    }

    public static int[] listIndexes(boolean[] mask) {
        int count = 0;
        for(int i=0; i<mask.length; i++) {
            if(mask[i])
                count++;
        }
        int[] indexes = new int[count];
        int curIndex = 0;
        for(int i=mask.length-1; i>=0; i--) {
            if(mask[i]) {
                indexes[curIndex] = i;
                curIndex++;
            }
        }
        return indexes;
    }

    public static int[] listIndexesF(boolean[] mask) {
        int count = 0;
        for(int i=0; i<mask.length; i++) {
            if(mask[i])
                count++;
        }

        int[] indexes = new int[count];
        int curIndex = 0;
        for(int i=mask.length-1; i>=0; i--) {
            if(mask[i]) {
                indexes[curIndex] = i;
                curIndex++;
            }
        }
        return indexes;
    }


    public static long totalToCheck() {
        long count = 0;
        for(boolean[] gMask : gBitMasks) {
            for(boolean[] fMask : fBitMasks) {
                long curCount = 1;
                int[] fIndexes = listIndexes(fMask);
                int[] gIndexes = listIndexes(gMask);
                int[] lockedIndex = getLockIndex(fMask, gMask);
                for(int index : fIndexes) {
                    if(index == lockedIndex[1] && lockedIndex[0] == 0) {
                        int numValues = fFGMapValues.get(index).size()-1;
                        curCount = curCount * numValues;
                    }
                    else {
                        curCount = curCount * (GF.n-1);
                    }
                }
                for(int index : gIndexes) {
                    if(index == lockedIndex[1] && lockedIndex[0] == 1) {
                        int numValues = gFGMapValues.get(index).size()-1;
                        curCount = curCount * numValues;
                    }
                    else {
                        curCount = curCount * (GF.n-1);
                    }
                }
                count += curCount;
            }
        }
        return count;
    }

    public static long totalSkipped(boolean[] fMask, boolean[] gMask) { // fractions we don't have to check because g(x) contains a 0
        long count = 0;
        long curCount = 1;
        int[] fIndexes = listIndexes(fMask);
        int[] lockedIndex = getLockIndex(fMask, gMask);
        for(int index : fIndexes) { // for each index in f (since we are skipping all f)
            if(index == lockedIndex[1] && lockedIndex[0] == 0) { // if f is locked, count the number of values for that index
                int numValues = fFGMapValues.get(index).size()-1;
                curCount = curCount * numValues;
            }
            else { // else we are skipping GF.n-1 values for that index
                curCount = curCount * (GF.n-1);
            }
        }
        count += curCount;
        return count;
    }

    public static void fixNumeratorDegrees() {
        Iterator<boolean[]> itr = fBitMasks.iterator();

        while(itr.hasNext()) {
            boolean[] mask = itr.next();
            boolean shouldRemove = false;

            for (int i = 0; i < fixedDegrees.size(); i++) { //each of these degrees have a + number fixed coefficient
                int coefIndex = fixedDegrees.get(i); //if the degree with 0 has a 1 in the mask

                if (mask[coefIndex]) {
                    shouldRemove = true;
                    break; // No need to check further, one match is enough to remove
                }
            }

            if (shouldRemove) {
                masksToRemove.add(mask); // Collect masks to be removed
            }
        }
    }




    static int[] createGPolynomial(boolean[] mask) {
        int[] poly = new int[mask.length];
        Arrays.fill(poly, 0);
        poly[0] = 1;
        for(int x=0; x<mask.length; x++) {
            if(mask[x]) {
                poly[x] = 1;
            }
        }
        return poly;
    }
    static int[] createFPolynomial(boolean[] mask) {
        int[] poly = new int[mask.length];
        Arrays.fill(poly, 0);
        poly[0] = 1;
        for(int x=0; x<mask.length; x++) {
            if(mask[x]) {
                poly[x] = 1;
            }
        }
        for(int i=0; i<fixedDegrees.size(); i++){ //fix given degrees to given indicies
            poly[fixedDegrees.get(i)] = fixedIndexes.get(i);
            fixNumeratorDegrees();
        }
        return poly;
    }

    static boolean incrementPolynomial(int[] poly, int[] maskIndexes, int fg) { // fg: 0=f, 1=g
        if(maskIndexes.length == 0) // no indexes to increment
            return false;
        int curMaskIndex = 0;
        int curIndex = maskIndexes[0];
        while(incrementIndex(poly, curIndex, fg) && curMaskIndex < maskIndexes.length) {
            curMaskIndex++;
            if(curMaskIndex == maskIndexes.length)
                return false;
            curIndex = maskIndexes[curMaskIndex];
        }
        return true;
    }

    static boolean incrementIndex(int[] poly, int index, int fg) { //return true if carries (does not include 0 values)
        if(index != lockIndex[1] || fg != lockIndex[0]) {
            poly[index]++;
            if(poly[index] == GF.n) {
                poly[index] = 1;
                return true;
            }
            else
                return false;
        }
        else {
            curLockValueIndex++;
            if(curLockValueIndex == lockValues.length) {
                curLockValueIndex = 1;
                poly[index] = lockValues[1];
                return true;
            }
            else {
                poly[index] = lockValues[curLockValueIndex];
                return false;
            }
        }
    }

    static HashMap<String, int[][]> getFGMaps(int[] f, int[] g) {
        int[] curF = Arrays.copyOf(f, f.length);
        int[] curG = Arrays.copyOf(g, g.length);
        String curFracString = Arrays.toString(f) + " / " + Arrays.toString(g);
        HashMap<String, int[][]> fMaps = new HashMap<>();
        //calculate fMaps
        while(!fMaps.containsKey(curFracString)) {
            fMaps.put(curFracString, new int[][] {curF, curG});
            curF = fMap(curF);
            curG = fMap(curG);
            curFracString = Arrays.toString(curF) + " / " + Arrays.toString(curG);
        }
        HashMap<String, int[][]> fgMaps = new HashMap<>(); //will store all fgMap combinations
        //calculate gMaps from each fMap
        for(String key : fMaps.keySet()) {
            curF = Arrays.copyOf(fMaps.get(key)[0], fMaps.get(key)[0].length);
            curG = Arrays.copyOf(fMaps.get(key)[1], fMaps.get(key)[1].length);
            curFracString = key;
            HashMap<String, int[][]> gMaps = new HashMap<>(); //all gMaps for the current fMap
            while(!gMaps.containsKey(curFracString)) {
                gMaps.put(curFracString, new int[][] {curF, curG});
                curF = gMap(curF);
                curG = gMap(curG);
                curFracString = Arrays.toString(curF) + " / " + Arrays.toString(curG);
            }
            fgMaps.putAll(gMaps);
        }
        return fgMaps;
    }

    public static int[] fMap(int[] p) {
        int[] result = new int[p.length];
        for(int i=0; i<p.length; i++)
            result[i] = mult(p[i], i+1);
        return result;
    }

    public static int[] gMap(int[] p) {
        int[] result = new int[p.length];
        for(int i=0; i<p.length; i++)
            result[i] = power(p[i], prime);
        return result;
    }

    static HashMap<String, int[][]> getFofXPlusBMaps(HashMap<String, int[][]> fgMaps) {
        HashMap<String, int[][]>  fofxpbMaps = new HashMap<>();
        for(String key : fgMaps.keySet()) {
            int[] curF = Arrays.copyOf(fgMaps.get(key)[0], fgMaps.get(key)[0].length);
            int[] curG = Arrays.copyOf(fgMaps.get(key)[1], fgMaps.get(key)[1].length);
            for(int b=0; b<GF.n; b++) {
                int[] fofxpb = fOfXPlusB(curF, b);
                int[] gofxpb = fOfXPlusB(curG, b);
                // normalize f(x) using H-map: f(x) + c*g(x) / g(x)
                int c = divide(subtract(0, fofxpb[fofxpb.length-1]), gofxpb[gofxpb.length-1]);
                int[] hmapfofxpb = addPoly(fofxpb, multA(gofxpb, c));
                String curFracString = Arrays.toString(hmapfofxpb) + " / " + Arrays.toString(gofxpb);
                fofxpbMaps.put(curFracString, new int[][] {hmapfofxpb, gofxpb});
            }
        }
        return fofxpbMaps;
    }

    public static void update() {
        float numMinutes = (float) (System.currentTimeMillis() - startTime) / 60000;
        DecimalFormat df = new DecimalFormat("##.##");
        float percent = (float)count / (float)totalToCheck;
        float totalMinutes = (float)numMinutes / (percent);
        System.out.println(df.format(percent*100)+"% complete. "+ df.format(numMinutes) + " min elapsed. Estimated "
                + df.format(totalMinutes-numMinutes)+" min remaining, "+df.format(totalMinutes)+" min total.");
    }

    public static int maxStringLength() {
        int length = 11; // "[1," + "] / [1," + "]"
        int digits = 3;
        if(GF.n < 100)
            digits = 2;
        if(GF.n < 10)
            digits = 1;
        length += (fdegree + gdegree) * (digits+2);
        return length;
    }

    public static void print(int[] array){
        System.out.println(Arrays.toString(array));
    }

    public static void parseArgs(String[] args) {
        if(args.length < 4) {
            System.out.println("Usage: java FracSearch <prime> <power> <f-degree> <g-degree>");
            System.out.println("f-degree must be strictly > g-degree");
            System.out.println("options:");
            System.out.println("     -v     verbose output of nFPPs");
            System.exit(0);
        }
        //initialize variables
        prime = Integer.parseInt(args[0]);
        power = Integer.parseInt(args[1]);
        fdegree = Integer.parseInt(args[2]);
        gdegree = Integer.parseInt(args[3]);
        GF.initGF(prime, power);
        verbose = false;
        /*if(fdegree <= gdegree) {
            System.out.println("f-degree must be strictly > g-degree");
            System.exit(0);
        }*/
        // check additional options
        fixedZeroDegrees = new ArrayList<Integer>();

        for (int i = 4; i+1 < args.length; i=i+2) {
            if(Integer.parseInt((args[i+1])) == 0){ //if it's a 0 coefficient
                fixedZeroDegrees.add(Integer.parseInt(args[i]));
            }
            else{
                fixedDegrees.add(fdegree-Integer.parseInt(args[i]));
                fixedIndexes.add(Integer.parseInt(args[i+1]));
            }
        }
        if(args.length > 4) {
            for(int x=4; x<args.length; x++) {
                switch(args[x]) {
                    case "-v":
                        verbose = true;
                        break;
                    default:
                        System.out.println("We are fixing some indicies.");
                }
            }
        }
        System.out.println("Here is the fixedZeroDegrees" + fixedZeroDegrees.toString());
    }
    public static void fixNumeratorDegreesToZero() {
        Iterator<boolean[]> itr = fBitMasks.iterator();
        while (itr.hasNext()) {
            boolean[] mask = itr.next();
            boolean shouldRemove = false;

            for (int zeroDegree : fixedZeroDegrees) {
                int coefIndex = fdegree - zeroDegree; // Correctly calculate the index for the array
                if (coefIndex < 0 || coefIndex >= mask.length) {
                    continue; // Skip if the index is out of bounds
                }
                if (mask[coefIndex]) {
                    mask[coefIndex] = false; // Instead of removing the mask, set the specific coefficient to false
                }
            }
        }
    }
}

class GF
{
    static Random rand = new Random(1);

    static int prime = 1;
    static int power = 1;
    static int n = 1;

    public static int[] addTable;
    public static int[] mulTable;

    public static Polynomial irr;

    public static Polynomial initGF(int prime, int power)
    {
            GF.prime = prime;
            GF.power = power;
            GF.n = (int) Math.pow(prime, power);

            Polynomial.mod = prime;
            irr = findRandomPrimitive(prime, power);
            List<Polynomial> arr = genPolynomials(prime, power, irr);

            addTable = addTable(prime, power, arr);
            mulTable = multTable(prime, power, irr, arr);

            return irr;
    }

    public static Polynomial findRandomPrimitive(int prime, int power)
    {
            int n = (int) Math.pow(prime, power);
            int[] temp = new int[n + 1];
            temp[temp.length - 1] = 1;
            temp[1] = prime - 1;
            Polynomial cur = randomPolynomial(prime, power);
            while (isReducible(cur, prime) || !isPrimitive(cur, n))
                    cur = randomPolynomial(prime, power);
            return cur;
    }

    public static boolean isPrimitive(Polynomial p, int n)
    {
            int d = n - 1;
            if (p.equals(Polynomial.monomial(1)))
                    return false;
            for (int x = 2; x <= n - 2; x++)
                    if (d % x == 0 && Polynomial.monomial(x).divide(p)[1].isOne())
                            return false;
            return true;
    }

    public static Polynomial randomPolynomial(int mod, int deg)
    {
            int[] coef = new int[deg + 1];
            coef[coef.length - 1] = 1;
            for (int i = 0; i < coef.length - 1; i++)
                    coef[i] = rand.nextInt(mod);
            return new Polynomial(coef);
    }

    public static List<Polynomial> genPolynomials(int prime, int power, Polynomial irr)
    {
            int n = (int) Math.pow(prime, power);
            Polynomial base = Polynomial.monomial(0);
            Polynomial x = Polynomial.monomial(1);
            Polynomial zero = Polynomial.zero();
            List<Polynomial> arr = new ArrayList<Polynomial>();
            arr.add(zero);
            do
            {
                    arr.add(base.divide(irr)[1]);
                    base = base.mult(x);
            }
            while (arr.size() < n);
            return arr;
    }

    public static boolean isReducible(Polynomial p, int mod)
    {
            for (int i = 1; i < p.deg; i++)
            {
                    int[] coef = new int[(int) Math.pow(mod, i) + 1];
                    coef[coef.length - 1] = 1;
                    coef[1] = mod - 1;
                    Polynomial test = new Polynomial(coef);
                    if (Polynomial.gcd(p, test).deg > 0)
                    {
                            return true;
                    }
            }
            return false;
    }

    public static int[] multTable(int prime, int pow, Polynomial irr, List<Polynomial> arr)
    {
            Polynomial.mod = prime;
            int sz = (int) Math.pow(prime, pow);
            int[] table = new int[sz * sz];
            for (int a = 0; a < sz; a++)
                    for (int b = 0; b < sz; b++)
                    {
                            Polynomial result = arr.get(a).mult(arr.get(b)).divide(irr)[1];
                            for (int i = 0; i < arr.size(); i++)
                                    if (arr.get(i).equals(result))
                                    {
                                            table[a + b * n] = i;
                                            break;
                                    }
                    }
            return table;
    }

    public static int[] addTable(int prime, int pow, List<Polynomial> arr)
    {
            Polynomial.mod = prime;
            int sz = (int) Math.pow(prime, pow);
            int[] table = new int[sz * sz];
            for (int a = 0; a < sz; a++)
                    for (int b = 0; b < sz; b++)
                    {
                            Polynomial result = arr.get(a).add(arr.get(b));
                            for (int i = 0; i < arr.size(); i++)
                            {
                                    if (arr.get(i).equals(result))
                                    {
                                            table[a + b * sz] = i;
                                            break;
                                    }
                            }
                    }
            return table;
    }
}

class Polynomial
{
    static int mod = 0;
    int[] coef;
    int deg;

    public Polynomial(int[] coef)
    {
            deg = coef.length - 1;
            while (deg >= 0 && coef[deg] == 0)
                    deg--;
            this.coef = Arrays.copyOf(coef, deg + 1);
    }

    public static Polynomial zero()
    {
            int[] coef = new int[1];
            coef[0] = 0;
            return new Polynomial(coef);
    }

    public static Polynomial monomial(int n)
    {
            int[] coef = new int[n + 1];
            coef[n] = 1;
            return new Polynomial(coef);
    }

    public boolean isOne()
    {
            return deg == 0 && coef[0] == 1;
    }

    public Polynomial mult(Polynomial other)
    {
            if (deg == -1 || other.deg == -1)
                    return new Polynomial(new int[] {});
            int[] newCoef = new int[deg + other.deg + 1];
            for (int i = 0; i < coef.length; i++)
                    for (int j = 0; j < other.coef.length; j++)
                            newCoef[i + j] = (coef[i] * other.coef[j] + newCoef[i + j]) % mod;
            return new Polynomial(newCoef);
    }

    public Polynomial add(Polynomial other)
    {
            int[] newCoef = new int[Math.max(coef.length, other.coef.length)];
            for (int i = 0; i < newCoef.length; i++)
            {
                    if (i < coef.length)
                            newCoef[i] = (newCoef[i] + coef[i]) % mod;
                    if (i < other.coef.length)
                            newCoef[i] = (newCoef[i] + other.coef[i]) % mod;
            }
            return new Polynomial(newCoef);
    }

    public Polynomial subtract(Polynomial other)
    {
            int[] newCoef = new int[Math.max(coef.length, other.coef.length)];
            for (int i = 0; i < newCoef.length; i++)
            {
                    if (i < coef.length)
                            newCoef[i] = (coef[i] + newCoef[i]) % mod;
                    if (i < other.coef.length)
                            newCoef[i] = (newCoef[i] - other.coef[i] + mod) % mod;
            }
            return new Polynomial(newCoef);
    }

    public Polynomial divideLeadTerms(Polynomial other)
    {
            int temp = deg - other.deg;
            int value = coef[coef.length - 1] * invert(other.coef[other.coef.length - 1]);
            int newCoef[] = new int[temp + 1];
            newCoef[temp] = value;
            return new Polynomial(newCoef);
    }

    public Polynomial[] divide(Polynomial divisor)
    {
            Polynomial[] res = new Polynomial[2];
            if (divisor.deg == -1)
                    return null;
            res[0] = new Polynomial(new int[] {});
            res[1] = this.copy();
            while (res[1].deg != -1 && res[1].deg >= divisor.deg)
            {
                    Polynomial temp = res[1].divideLeadTerms(divisor);
                    res[0] = res[0].add(temp);
                    res[1] = res[1].subtract(temp.mult(divisor));
            }
            return res;
    }

    public static int invert(int n)
    {
            for (int a = 1; a < mod; a++)
                    if ((a * n) % mod == 1)
                            return a;
            return -1;
    }

    public static Polynomial gcd(Polynomial a, Polynomial b)
    {
            if (b.deg == -1)
                    return a;
            else
                    return gcd(b, a.divide(b)[1]);
    }

    public Polynomial copy()
    {
            int[] newCoef = Arrays.copyOf(coef, coef.length);
            return new Polynomial(newCoef);
    }

    public boolean equals(Object other)
    {
            if (other instanceof Polynomial)
            {
                    Polynomial poly = (Polynomial) other;
                    if (poly.deg != deg)
                            return false;
                    for (int i = 0; i < coef.length; i++)
                            if (coef[i] != poly.coef[i])
                                    return false;
                    return true;
            }
            return false;
    }

    public String toString()
    {
            StringBuilder sb = new StringBuilder();
            if (deg == -1)
                    return "0";
            for (int i = coef.length - 1; i >= 2; i--)
            {
                    if (coef[i] == 1)
                            sb.append("x^" + i + " + ");
                    else if (coef[i] != 0)
                            sb.append(coef[i] + "x^" + i + " + ");
            }
            if (coef.length >= 2 && coef[1] != 0)
                    sb.append((coef[1] == 1 ? "" : coef[1]) + "x + ");
            if (coef.length >= 1 && coef[0] != 0)
                    sb.append(coef[0]);
            else
                    sb.delete(sb.length() - 3, sb.length());
            return sb.toString();
    }
}