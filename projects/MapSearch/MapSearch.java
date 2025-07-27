import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class MapSearch {
    static int prime;
    static int power;
    static int messageLength;
    static int degree;
    static int[][] powerTable;
    static boolean degModPrime;
    
    static HashMap<Integer, ArrayList<Integer>> indexElements;
    static int lockIndex;
    static int[] lockValues;
    static int curLockValueIndex;
    
    static boolean resume;
    
    static ArrayList<boolean[]> bitMasks;
    static ArrayList<boolean[]> bitMasksDMP; //additional masks for when Degree % Prime = 0
    
    public static void main(String[] args) throws IOException {
        parseArgs(args);
        //prime = 2;
        //power = 6;
        //degree = 10;
        //messageLength = degree + 1;    
        //GF.initGF(prime, power);
        
        long startTime = System.currentTimeMillis();
        powerTable = fillPowerTable();
        if(degree % prime == 0)
            degModPrime = true;
        else degModPrime = false;
        resume = checkResume();
        System.out.println(GF.irr+"\n");
        
        ArrayList<ArrayList<Integer>> gOrbits = getGOrbits();
        indexElements = getIndexElements(gOrbits); //Minimum elements we need to check if we lock the given index (F and G Maps)
        System.out.println("Minimum FG-Map Elements by Index");
        for(int key : indexElements.keySet()) {
            System.out.print(key+": "+indexElements.get(key));
            if(key == 0 || (key == 1 && !degModPrime) || key == messageLength-1)
                System.out.print(" (Locking)");
            System.out.println();
        }
        System.out.println();
        
        boolean gf64deg10 = (GF.n == 64 && degree == 10); //flag for special case 64deg10
        bitMasks = createBitMasks();
        if(degModPrime && prime > 2)
            bitMasksDMP = createBitMasksDMP3();
        if(degModPrime && prime == 2 && !gf64deg10) {
            ArrayList<boolean[]> bitMasks2 = createBitMasks();
            boolean[] allFalse = bitMasks2.get(0).clone();
            allFalse[1] = true;
            allFalse[allFalse.length-2] = false;
            bitMasks.add(allFalse); //manually create and add [F, T, F, ..., F]
            for(boolean[] mask : bitMasks2)
                mask[1] = true;
            bitMasks.addAll(bitMasks2);
        }
        //Special case for GF64 degree 10, either deg7 or deg6 coeff is 0
        if(gf64deg10) {
            //swap deg9 and deg7 coeff from normal masks we create, since we will first lock deg7 at 0
            for(boolean[] mask : bitMasks) {
                boolean temp = mask[1]; //deg9
                mask[1] = mask[3];
                mask[3] = temp;
            }
            bitMasksDMP = createBitMasksDMP3(); //create the DMP3 bitmasks, but swap deg9/7 and deg8/6
            for(boolean[] mask : bitMasksDMP) {
                boolean temp = mask[1]; //deg9
                mask[1] = mask[3];
                mask[3] = temp;  
                temp = mask[2];
                mask[2] = mask[4];
                mask[4] = temp;
            }
        }
        
        /*System.out.println("Normal Masks: "+bitMasks.size());
        for(boolean[] mask : bitMasks) {
            System.out.println(Arrays.toString(mask));
        }
        
        if((degModPrime && prime > 2) || gf64deg10) {
            System.out.println("\nDMP Masks: "+bitMasksDMP.size());
            for(boolean[] mask : bitMasksDMP) {
                System.out.println(Arrays.toString(mask));
            }
        }*/
        
        HashSet<String> foundPPs = new HashSet<>();
        long totalToCheck = totalToCheck();
        long lastMinuteStart = System.currentTimeMillis();
        float numMinutes = 0;
        long count = 1;
        BufferedWriter outFile = null;
        String outputFile = prime+"_"+power+"_"+"deg"+degree+".txt";
        
        //Resume Search Logic
        boolean resumeValuesLoaded = false;
        long resumeCount = 0l;
        boolean[] resumeMask = null;
        int[] resumePP = null;
        int curCLVI = 0;
        boolean skipToDMPLoop = false;
        float startPercent = 0;
        
        if(resume) { //load variables and open output file for appending
            String saveFileName = prime+"_"+power+"_"+"deg"+degree+"_save.txt";
            BufferedReader saveReader = new BufferedReader(new FileReader(saveFileName));
            String firstLine = saveReader.readLine();
            if(firstLine.equals("complete")) {
                System.out.println("A completed search has been detected. See \""+outputFile+"\" for results.");
                System.out.println("Delete \""+saveFileName+"\" if you wish to start a new search");
                System.exit(0);
            }
            resumeCount = Long.parseLong(firstLine);
            resumeMask = parseMask(saveReader.readLine());
            resumePP = parsePP(saveReader.readLine());
            curCLVI = Integer.parseInt(saveReader.readLine());
            skipToDMPLoop = Boolean.parseBoolean(saveReader.readLine());
            startPercent = (float)resumeCount / (float)totalToCheck;
            System.out.println("Resuming Search From:");
            System.out.println("count = "+resumeCount);
            System.out.println("mask = "+Arrays.toString(resumeMask));
            System.out.println("pp = "+Arrays.toString(resumePP));
            System.out.println("curLockValueIndex = "+curCLVI);
            System.out.println("skipToDMPLoop = "+skipToDMPLoop);
            //Remove masks that have already been checked according to if we are in main loop or DMP loop
            boolean foundMask = false;
            ArrayList<boolean[]> newMasks = new ArrayList<>();
            ArrayList<boolean[]> curMasks = new ArrayList<>();
            if(skipToDMPLoop)
                curMasks = bitMasksDMP;
            else
                curMasks = bitMasks;
            for(int i=0; i< curMasks.size(); i++) {
                if(!foundMask) { 
                    if(Arrays.toString(resumeMask).equals(Arrays.toString(curMasks.get(i)))) { //start with resumeMask
                        newMasks.add(curMasks.get(i));
                        foundMask = true;
                    }
                }
                else { //add all masks that come after resumeMask
                    newMasks.add(curMasks.get(i));
                }                
            }
            if(skipToDMPLoop) {
                bitMasks = new ArrayList<>();
                bitMasksDMP = newMasks;
            }
            else
                bitMasks = newMasks;
            //load previously found PPs from file
            foundPPs = readPPFile();
            //open output file with append flag to continue search
            try {
                outFile = new BufferedWriter(new FileWriter(outputFile, true));
            } catch (IOException ex) {
                System.out.println("Error creating output file.");
                System.exit(0);
            }
        }
        else { //create new output file and write primitive poly
            try {
                outFile = new BufferedWriter(new FileWriter(outputFile));
            } catch (IOException ex) {
                System.out.println("Error creating output file.");
                System.exit(0);
            }
            outFile.write(GF.irr+"\r\n");
            outFile.flush();
        }
        
        //Manually check [1, 0, 0, ..., 0] since we only loop on non-zero coefficients
        if(!resume) { //only check if we are not resuming a previous search
            int[] zeroPP = new int[messageLength];
            Arrays.fill(zeroPP, 0);
            zeroPP[0] = 1;
            if(checkPerm(zeroPP)) {
                foundPPs.add(Arrays.toString(zeroPP));
                String output = Arrays.toString(zeroPP);
                outFile.write(output+"\r\n");
                outFile.flush();           
                while(output.length() < maxStringLength())
                    output += " ";
                System.out.println(output +"   "+1+" NPPs Found. "+foundPPs.size()+" Total.");
            }
        }
        
        //Main Loop
        for(boolean[] mask : bitMasks) {
            //Find lock index
            lockIndex = getLockIndex(mask);
            lockValues = indexElements.get(lockIndex).stream().mapToInt(i->i).toArray();
            curLockValueIndex = 1;  //skipping value 0 at index 0
            //Create initial pp
            int[] pp = new int[messageLength];
            Arrays.fill(pp, 0);
            pp[0] = 1;
            for(int x=0; x<mask.length; x++) {
                if(mask[x]) {
                    pp[x] = 1;
                }
            }
            //Set maskIndexes
            int[] maskIndexes = listIndexes(mask);
            //System.out.println("maskIndexes: "+ Arrays.toString(maskIndexes));
            
            //Load values from our save file if we are resuming a search
            if(resume && !resumeValuesLoaded) {
                pp = resumePP;
                count = resumeCount;
                curLockValueIndex = curCLVI;
                resumeValuesLoaded = true;
            }
           
            do {
                count++;
                if(checkPerm(pp) && !foundPPs.contains(Arrays.toString(pp))) {
                    HashMap<String, int[]> fgMaps = getFGMaps(pp);
                    String toWrite = "";
                    for(String key : fgMaps.keySet())
                        toWrite += key + "\r\n";
                    outFile.write(toWrite);
                    outFile.flush();
                    saveProgress(count, mask, pp, false);
                    foundPPs.addAll(fgMaps.keySet());
                    String output = Arrays.toString(pp);                     
                    while(output.length() < maxStringLength())
                        output += " ";
                    System.out.println(output+"   "+fgMaps.size()+" NPPs Found. "+foundPPs.size()+" Total.");
                }
                if(System.currentTimeMillis()-lastMinuteStart >= 60000) {
                    saveProgress(count, mask, pp, false);
                    lastMinuteStart = System.currentTimeMillis();
                    numMinutes = (System.currentTimeMillis() - startTime) / 60000;
                    DecimalFormat df = new DecimalFormat("##.##");
                    float percent = (float)count / (float)totalToCheck;
                    float totalMinutes = (float)numMinutes / (percent-startPercent) * (1-startPercent); //adjusted by startPercent if we resume
                    System.out.println(df.format(percent*100)+"% complete. "+ df.format(numMinutes) + " min elapsed. Estimated " + df.format(totalMinutes-numMinutes)+" min remaining, "+df.format(totalMinutes)+" min total.");
                }                
            }
            while(incrementPoly(pp, maskIndexes));
        }
        if((degModPrime && prime > 2) || gf64deg10) { //check cases [1, X, 0, ..., 0]
            for(boolean[] mask : bitMasksDMP) {
                //Find lock index
                lockIndex = getLockIndex(mask);
                lockValues = indexElements.get(lockIndex).stream().mapToInt(i->i).toArray();
                curLockValueIndex = 1;  //skipping value 0 at index 0
                //Create initial pp
                int[] pp = new int[messageLength];
                Arrays.fill(pp, 0);
                pp[0] = 1;
                for(int x=0; x<mask.length; x++) {
                    if(mask[x]) {
                        pp[x] = 1;
                    }
                }
                //Set maskIndexes
                int[] maskIndexes = listIndexes(mask);
                //System.out.println("maskIndexes: "+ Arrays.toString(maskIndexes));
                
                //Load values from our save file if we are resuming a search
                if(resume && !resumeValuesLoaded) {
                    pp = resumePP;
                    count = resumeCount;
                    curLockValueIndex = curCLVI;
                    resumeValuesLoaded = true;
                }          
                
                do {
                   count++;
                   if(checkPerm(pp) && !foundPPs.contains(Arrays.toString(pp))) {
                       HashMap<String, int[]> fgMaps = getFGMaps(pp);
                       String toWrite = "";
                       HashMap<String, int[]> fgbMaps = new HashMap<>(); //will store all fg(x+b)Map combinations
                       for(int[] curPP : fgMaps.values()) {
                           HashMap<String, int[]> fofxpbMaps = getFofXPlusBMaps(curPP);
                           fgbMaps.putAll(fofxpbMaps);
                       }
                       for(String key : fgbMaps.keySet())
                           toWrite += key + "\r\n";
                       outFile.write(toWrite);
                       outFile.flush();
                       saveProgress(count, mask, pp, true);
                       foundPPs.addAll(fgbMaps.keySet());
                       String output = Arrays.toString(pp);                     
                       while(output.length() < maxStringLength())
                           output += " ";
                       System.out.println(output+"   "+fgbMaps.size()+" NPPs Found. "+foundPPs.size()+" Total.");
                   }
                   if(System.currentTimeMillis()-lastMinuteStart >= 60000) {
                       saveProgress(count, mask, pp, true);
                       lastMinuteStart = System.currentTimeMillis();
                       numMinutes = (System.currentTimeMillis() - startTime) / 60000;
                       DecimalFormat df = new DecimalFormat("##.##");
                       float percent = (float)count / (float)totalToCheck;
                       float totalMinutes = (float)numMinutes / (percent-startPercent) * (1-startPercent); //adjusted by startPercent if we resume
                       System.out.println(df.format(percent*100)+"% complete. "+ df.format(numMinutes) + " min elapsed. Estimated " + df.format(totalMinutes-numMinutes)+" min remaining, "+df.format(totalMinutes)+" min total.");
                   }
               }
               while(incrementPoly(pp, maskIndexes));        
            } 
        }
        outFile.close();
        saveProgressComplete();
        long endTime = System.currentTimeMillis();
        DecimalFormat df = new DecimalFormat("##.##");
        System.out.println("\nSearch Complete");
        System.out.println(df.format((float)(endTime-startTime)/60000) + " min elapsed");
        System.out.println(foundPPs.size()+" NPPs Found");
    }

    public static int add(int a, int b) { //a + b in GF
        return GF.addTable[a + b * GF.n];
    }    
    
    public static int mult(int a, int b) { //a * b in GF
        return GF.mulTable[a + b * GF.n];
    }
    
    public static int[][] fillPowerTable() {
        int maxDegree = Math.max(messageLength, prime+1);
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
    
    public static int power(int a, int b) { //a^b in GF
        return powerTable[a][b];
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
    
    public static HashMap<Integer, ArrayList<Integer>> getIndexElements(ArrayList<ArrayList<Integer>> gOrbits) {
        HashMap<Integer, ArrayList<Integer>> indexElements = new HashMap<>();
        indexElements.put(0, new ArrayList<>(Arrays.asList(1)));
        indexElements.put(1, new ArrayList<>(Arrays.asList(0)));
        indexElements.put(messageLength-1, new ArrayList<>(Arrays.asList(0)));
        int startIndex = 2;
        if(degModPrime)
            startIndex = 1;
        for(int i=startIndex; i<messageLength-1; i++) {
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
                    //System.out.println("adding "+fullOrbitArray[0]);

                    
                    
                    //System.out.println("Field: "+field.toString());
                }
            }
            indexElements.put(i, toAdd);
            //System.out.println("printing "+toAdd);
        }           
        return indexElements;
    }
    
    public static ArrayList<boolean[]> createBitMasks() {
        ArrayList<boolean[]> masks = new ArrayList<>();
        for(int x=1; x<Math.pow(2,messageLength-3); x++) {
            long[] l = {x};
            BitSet bs = BitSet.valueOf(l);
            boolean[] mask = new boolean[messageLength];
            Arrays.fill(mask, false);
            for(int i=0; i<bs.length(); i++) {
                if(bs.get(i)) {
                    mask[messageLength-2-i] = true;
                }
            }
            masks.add(mask);
        }
        return masks;
    }

    public static ArrayList<boolean[]> createBitMasksDMP3() { //Bit masks when Prime > 2 and Degree is multiple of Prime
        ArrayList<boolean[]> masks = new ArrayList<>();
        for(int x=0; x<Math.pow(2,messageLength-4); x++) {
            long[] l = {x};
            BitSet bs = BitSet.valueOf(l);
            boolean[] mask = new boolean[messageLength];
            Arrays.fill(mask, false);
            for(int i=0; i<bs.length(); i++) {
                if(bs.get(i)) {
                    mask[messageLength-2-i] = true;
                }
            }
            mask[1] = true;
            masks.add(mask);
        }
        return masks;
    }    
    
    public static int getLockIndex(boolean[] mask) {
        int smallestIndex = -1;
        int smallestSize = Integer.MAX_VALUE;
        for(int x=1; x<messageLength-1; x++) {
            if(indexElements.get(x).size() < smallestSize && mask[x]) {
                smallestIndex = x;
                smallestSize = indexElements.get(x).size();
            }
        }        
        return smallestIndex;
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
    
    public static long totalToCheck() {
        long count = 1; //[1, 0, 0, ..., 0] not in masks, so count manually
        for(boolean[] mask : bitMasks) {
            long curCount = 1;
            int[] indexes = listIndexes(mask);
            int smallestIndex = getLockIndex(mask);
            for(int index : indexes) {
                if(index == smallestIndex) {
                    int numNalues = indexElements.get(index).size()-1;
                    curCount = curCount * numNalues;
                }
                else {
                    curCount = curCount * (GF.n-1);
                }
            }
            count += curCount;  
        }
        if(degModPrime && prime > 2 || (GF.n == 64 && degree == 10)) {
            for(boolean[] mask : bitMasksDMP) {
                long curCount = 1;
                int[] indexes = listIndexes(mask);
                int smallestIndex = getLockIndex(mask);
                for(int index : indexes) {
                    if(index == smallestIndex) {
                        int numNalues = indexElements.get(index).size()-1;
                        curCount = curCount * numNalues;
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
    
    public static boolean checkPerm(int[] message) {
        HashSet<Integer> values = new HashSet<>();
        values.add(message[message.length-1]); //add constant term for x=0
        for(int x=1; x<GF.n; x++) {
            int sum = 0;
            for(int y=0; y<messageLength; y++) {
                if(message[y] != 0) {
                    int pow = power(x, degree-y); //raise x to degree of term y in the PP
                    sum = add(sum, mult(pow, message[y])); //multiply pow by coeff of term y, and increment sum by result
                }
            }
            if(!values.add(sum))
                return false;
        }
        return true;
    }
    
//    static boolean incrementPoly(int[] poly) {
//        int curIndex = messageLength-2;
//        while(incrementIndex(poly, curIndex) && curIndex >=2) {
//            curIndex--;
//            if(curIndex == 1)
//                return false;
//        }
//        return true;
//    }
    
    static boolean incrementPoly(int[] poly, int[] maskIndexes) {
        int curMaskIndex = 0;
        int curIndex = maskIndexes[0];
        while(incrementIndexNoZero(poly, curIndex) && curMaskIndex < maskIndexes.length) {
            curMaskIndex++;
            if(curMaskIndex == maskIndexes.length)
                return false;
            curIndex = maskIndexes[curMaskIndex];
        }
        return true;
    }
    
//    static boolean incrementIndex(int[] poly, int index) { //return true if carries
//        if(index != lockIndex) {
//            poly[index]++;
//            if(poly[index] == GF.n) {
//                poly[index] = 0;
//                return true;
//            }
//            else
//                return false;
//        }
//        else {
//            curLockValueIndex++;
//            if(curLockValueIndex == lockValues.length) {
//                curLockValueIndex = 0;
//                poly[index] = lockValues[0];
//                return true;
//            }
//            else {
//                poly[index] = lockValues[curLockValueIndex];
//                return false;
//            }
//        }
//    }
    
    static boolean incrementIndexNoZero(int[] poly, int index) { //return true if carries (does not include 0 values)
        if(index != lockIndex) {
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

    static HashMap<String, int[]> getFGMaps(int []pp) {
        int[] curPP = Arrays.copyOf(pp, pp.length);
        String curPPString = Arrays.toString(pp);                        
        HashMap<String, int[]> fMaps = new HashMap<>();
        //calculate fMaps
        while(!fMaps.containsKey(curPPString)) {
            fMaps.put(curPPString, curPP);
            curPP = fMap(curPP);
            curPPString = Arrays.toString(curPP);
        }
        HashMap<String, int[]> fgMaps = new HashMap<>(); //will store all fgMap combinations
        //calculate gMaps from each fMap
        for(String key : fMaps.keySet()) {
            curPP = Arrays.copyOf(fMaps.get(key), fMaps.get(key).length);
            curPPString = key;
            HashMap<String, int[]> gMaps = new HashMap<>(); //all gMaps for the current fMap
            while(!gMaps.containsKey(curPPString)) {
                gMaps.put(curPPString, curPP);
                curPP = gMap(curPP);
                curPPString = Arrays.toString(curPP);
            }
            fgMaps.putAll(gMaps);
        }
        return fgMaps;
    }
    
    static HashMap<String, int[]> getFofXPlusBMaps(int []pp) {
        HashMap<String, int[]> fofxpbMaps = new HashMap<>();
        for(int b=0; b<GF.n; b++) {
            int[] fofxpb = fOfXPlusBNormal(pp, b);
            fofxpbMaps.put(Arrays.toString(fofxpb), fofxpb);
        }
        return fofxpbMaps; 
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
    
    public static int[] fOfXPlusBNormal(int[] p, int b) { //same as f(x+b) but normalizes result by zeroing out constant term
        int[] result = {p[p.length-1]}; //start with result as constant of p
        for(int i=p.length-2; i>=0; i--) {
            int[] binomial = {1, b}; //(x+b)
            int[] poly = binomialToPower(binomial, b, p.length-1-i); //(x+b)^p.length-1-i
            int[] timesCoef = multA(poly, p[i]); // a*(x+b)^p.length-1-i
            result = addPoly(timesCoef, result);
        }
        result[result.length-1]=0; //normalize result (+c to 0 out constant term)
        return result;
    }

    public static int[] binomialToPower(int[] p1, int b, int n) { //p1 = (x+b), return (x+b)^n
        if(n == 1)
            return p1;
        else {
            int[] timesX = multX(p1);
            int[] timesB = multA(p1, b);
            int[] result = addPoly(timesX, timesB);
            return binomialToPower(result, b, n-1);
        }
    }    
    
        public static int[] multX(int[] p) { //multiply polynomial p by x
        int[] px = new int[p.length+1];
        px[px.length-1] = 0;
        System.arraycopy(p, 0, px, 0, p.length);
        return px;
    }
    
    public static int[] multA(int[] p, int a) { //multipy polynomial p by a
        int[] pa = Arrays.copyOf(p, p.length);
        for(int i=0; i<pa.length; i++)
            pa[i]=mult(p[i],a);
        return pa;
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
    
    public static int maxStringLength() {
        int length = 9; // "[1, 0," +  " 0]"
        int digits = 3;
        if(GF.n < 100)
            digits = 2;
        if(GF.n < 10)
            digits = 1;
        length += (messageLength-3) * (digits+2);
        return length;
    }
    
    public static void parseArgs(String[] args) {
        if(args.length != 3) {
            System.out.println("Usage: java MapSearch <prime> <power> <degree>");
            System.exit(0);
        }
        prime = Integer.parseInt(args[0]);
        power = Integer.parseInt(args[1]);
        degree = Integer.parseInt(args[2]);
        messageLength = degree+1;
        GF.initGF(prime, power); 
    }
    
    public static boolean checkResume() {
        String saveFile = prime+"_"+power+"_"+"deg"+degree+"_save.txt";
        File testFile = new File(saveFile);
        return testFile.exists();
    }

    public static void saveProgress(long curCount, boolean[] curMask, int[] curPP, boolean skipToDMPLoop) throws IOException {
        String saveFileName = prime+"_"+power+"_"+"deg"+degree+"_save.txt";
        BufferedWriter saveFile = null;
        try {
            saveFile = new BufferedWriter(new FileWriter(saveFileName));
        } catch (IOException ex) {
            System.out.println("Error creating save file.");
            System.exit(0);
        }
        saveFile.write(curCount+"\r\n");
        saveFile.write(Arrays.toString(curMask)+"\r\n");
        saveFile.write(Arrays.toString(curPP)+"\r\n");
        saveFile.write(curLockValueIndex+"\r\n");
        saveFile.write(skipToDMPLoop+"\r\n");
        saveFile.close();
    }
    
    public static void saveProgressComplete() throws IOException {
        String saveFileName = prime+"_"+power+"_"+"deg"+degree+"_save.txt";
        BufferedWriter saveFile = null;
        try {
            saveFile = new BufferedWriter(new FileWriter(saveFileName));
        } catch (IOException ex) {
            System.out.println("Error creating save file.");
            System.exit(0);
        }
        saveFile.write("complete\r\n");
        saveFile.close();        
    }

    public static boolean[] parseMask(String input) {
        input = input.replace("[", ""); //remove unnecessary characters
        input = input.replace("]", "");
        input = input.replace(",", "");
        String[] split = input.split(" "); //split string into array
        boolean[] mask = new boolean[split.length];
        for(int x=0; x<split.length; x++) {
            mask[x] = Boolean.parseBoolean(split[x]);
        }
        return mask;        
    }

    private static int[] parsePP(String input) {
        input = input.replace("[", ""); //remove unnecessary characters
        input = input.replace("]", "");
        input = input.replace(",", "");
        String[] split = input.split(" "); //split string into array
        int[] pp = new int[split.length];
        for(int x=0; x<split.length; x++) {
            pp[x] = Integer.valueOf(split[x]);
        }
        return pp;
    }

    public static HashSet<String> readPPFile() {
        String fileName = prime+"_"+power+"_"+"deg"+degree+".txt";
        HashSet<String> ppSet = new HashSet<>();
        try {
            BufferedReader ppFile = new BufferedReader(new FileReader(fileName));   
            String currentLine = ppFile.readLine(); //read first line to disregard (primitive poly)
            while((currentLine = ppFile.readLine()) != null) { //expected format "[x1, x2, ..., xn]"  
                if(!currentLine.isEmpty()) { //prevent errors from empty lines
                    ppSet.add(currentLine);
                }
            }
            ppFile.close();
        } catch (IOException ex) {
            System.out.println("Error reading Permutation Polynomial file \""+fileName+"\", or file not found.");
            System.exit(0);
        }
        return ppSet;
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