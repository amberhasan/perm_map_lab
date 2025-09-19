import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class PermGen {
    // Parameter Variables
    static int prime;
    static int power;
    static String inputFileName;
    static int fdegree;
    static int gdegree;
    static boolean onlyNormPerms;
    static boolean noBNorm;
    
    // Arithmetic Tables
    static int[][] subtractionTable;
    static int[][] divisionTable;
    static int[][] powerTable;
    
    // Program Variables
    static String outputFileName;
    static boolean prfFile;
    static ArrayList<int[][]> prfArray;

    public static void main(String[] args) {
        parseArgs(args);        
        System.out.println(GF.irr+"\n");
        subtractionTable = fillSubtractionTable();
        divisionTable = fillDivisionTable();
        powerTable = fillPowerTable();
        
        // Configure ranges for a*f(x+b)+c
        int aRange = GF.n;
        int bRange = GF.n;
        int cRange = GF.n;
        if(prfFile) {
            if(gdegree % prime == 0) // PRFs use f(x+b) normalization on denominator
                bRange = 1;
        }
        else {
            if(fdegree % prime == 0) // PPs (as a degree f/0 PRF) use normalization on numerator
                bRange = 1;
        }
        if(onlyNormPerms) {
            aRange = 2;
            bRange = 1;
            cRange = 1;
        }
        
        int count = 0;
        BufferedWriter outFile = null;
        try {
            outFile = new BufferedWriter(new FileWriter(outputFileName));
            for(int[][] prf : prfArray) {
                for(int b=0; b<bRange; b++) {
                    int[] fValues = evaluatePolynomial(fOfXPlusB(prf[0], b));
                    int[] gValues = new int[GF.n];
                    Arrays.fill(gValues, 1);
                    if(prfFile)
                        gValues = evaluatePolynomial(fOfXPlusB(prf[1], b));
                    int[] initialPerm = new int[GF.n];
                    for(int i=0; i<GF.n; i++)
                        initialPerm[i] = divide(fValues[i], gValues[i]);                  
                    for(int a=1; a<aRange; a++) {
                        int[] multPerm = multArray(initialPerm, a); //multiply initial permutation by coeff 'a'
                        for(int c=0; c<cRange; c++) {
                            int[] finalPerm = addArray(multPerm, c); //add 'c' to each term in the multiplied permutation
                            String permString = Arrays.toString(finalPerm).replaceAll("[\\[,\\]]", "");
                            outFile.write(permString + "\r\n");
                            count++;
                        }
                    }
                }
            }
            outFile.close();
        } catch (IOException ex) {
                System.out.println("Output file error. " + outputFileName);
                ex.printStackTrace();
        }
    
        System.out.println(count + " permutations written to \"" + outputFileName + "\"");
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
    
    public static int[][] dividePoly(int[] a, int[] b) { // returns polynomial a / polynomial b, result[0] = quotient, result[1] = remainder
        // Euclidean Division: https://en.wikipedia.org/wiki/Polynomial_greatest_common_divisor#Euclidean_division
        int[] q = {0};
        int[] r = Arrays.copyOf(a, a.length);
        int d = b.length - 1;
        if(d == 0) { // b is not a polynomial, do coefficient division
            int[][] result = new int[2][];
            result[0] = divideA(a, b[0]);
            result[1] = new int[] {0};            
            return result;
        }
        int c = b[0];
        while(r.length-1 >= d) {
            int[] s = new int[r.length - d];
            Arrays.fill(s, 0);
            s[0] = divide(r[0], c);
            q = addPoly(q, s);
            int[] sb = multPoly(s, b);
            r = subtractPoly(r, sb);
        }
        int[][] result = new int[2][];
        result[0] = q;
        result[1] = r;
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
    
    public static int[] multArray(int[] array, int coeff) {
        int[] resultArray = new int[array.length];
        for(int x=0; x<resultArray.length; x++)
            resultArray[x] = mult(array[x], coeff);
        return resultArray;
    }
    
    public static int[] addArray(int[] array, int coeff) {
        int[] resultArray = new int[array.length];
        for(int x=0; x<resultArray.length; x++)
            resultArray[x] = add(array[x], coeff);
        return resultArray;
    }
    
//    public static boolean incrementPolynomial(int[] polynomial, int stopIndex) { // stopIndex = -1 for all indexes, 0 if poly is monic, etc.
//        int curIndex = polynomial.length - 1;
//        while(incrementIndex(polynomial, curIndex)) {
//            curIndex--;
//            if(curIndex == stopIndex)
//                return false;
//        }
//        return true;
//    }
//
//    public static boolean incrementIndex(int[] polynomial, int index) { //return true if carries
//        polynomial[index]++;
//        if(polynomial[index] == GF.n) {
//            polynomial[index] = 0;
//            return true;
//        }
//        else
//            return false;
//    }
    
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
    
    public static int[] calcPerm(int[] f, int[] gValues) {
        int[] permutation = new int[GF.n];
        for(int x=0; x<GF.n; x++) {
            int fValue = evaluateX(x, f);
            permutation[x] = divide(fValue, gValues[x]);
        }
        return permutation;
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
    
    
   
    static int[] createPolynomial(boolean[] mask) {
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
    
    public static ArrayList<int[][]> readInputFile(String fileName) { 
        ArrayList<int[][]> prfs = new ArrayList<>();
        try {
            BufferedReader inputFile = new BufferedReader(new FileReader(fileName)); 
            inputFile.readLine();
            String currentLine;
            while((currentLine = inputFile.readLine()) != null) {
                if(prfFile) {
                    String[] split = currentLine.split(" / ");
                    int[] f = polyStringToArray(split[0]);
                    int[] g = polyStringToArray(split[1]);
                    int[][] prf = new int[2][];
                    prf[0] = f;
                    prf[1] = g;
                    prfs.add(prf);
                }
                else {
                    int[] f = polyStringToArray(currentLine);
                    int[] g = {1};
                    int[][] prf = new int[2][];
                    prf[0] = f;
                    prf[1] = g;
                    prfs.add(prf);
                }
            }
            inputFile.close();
        } catch (IOException ex) {
            System.out.println("Error reading Permutation Polynomial file \""+fileName+"\", or file not found.");
            System.exit(0);
        }
        return prfs;
    }
    
    public static int[] polyStringToArray(String polyString) {
        String[] split = polyString.trim().replaceAll("[\\[,\\]]", "").split(" ");
        int[] polynomial = new int[split.length];
        for(int x=0; x<split.length; x++) {
            polynomial[x] = Integer.valueOf(split[x]);
        }
        return polynomial;
    }
    
    public static void parseArgs(String[] args) {
        if(args.length < 1 || args.length > 2) {
            System.out.println("Usage: java PermGen <inputfile> -options");
            System.out.println("options:");
            System.out.println("     -norm     generates only permutations from the normalized PPs/PRFs in the input file");            
            System.exit(0);
        }
        //initialize variables
        onlyNormPerms = false;
        if(args.length == 2) {
            if(args[1].equals("-norm"))
                onlyNormPerms = true;
            else {
                System.out.println(args[1] + " is an unrecognized option.");
                System.out.println("Usage: java PermGen <inputfile> -options");
                System.out.println("options:");
                System.out.println("     -norm     generates only permutations from the normalized PPs/PRFs in the input file");            
                System.exit(0);
            }
        }
        prfFile = args[0].startsWith("frac_");
        String[] params = null;
        if(prfFile)
            params = args[0].replace("frac_", "").replace(".txt", "").split("_");
        else
            params = args[0].replace("deg", "").replace(".txt", "").concat("_0").split("_");
        
        prime = Integer.parseInt(params[0]);
        power = Integer.parseInt(params[1]);
        fdegree = Integer.parseInt(params[2]);
        gdegree = Integer.parseInt(params[3]);
        GF.initGF(prime, power);
        
        prfArray = readInputFile(args[0]);
        outputFileName = args[0].replace(".txt", "") + "_perms.txt";
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