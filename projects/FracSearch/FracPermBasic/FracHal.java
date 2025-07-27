import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class FracHal {
    static int prime;
    static int power;
    static int fdegree;
    static int gdegree;
    
    static int[][] subtractionTable;
    static int[][] divisionTable;
    static int[][] powerTable;
    
    static boolean outputPerms;

    public static void main(String[] args) throws IOException {
        parseArgs(args);
        //prime = 3;
        //power = 2;
        //fdegree = 3;
        //gdegree = 2;
        //GF.initGF(prime, power);
        //outputPerms = true;
        System.out.println(GF.irr+"\n");
        subtractionTable = fillSubtractionTable();
        divisionTable = fillDivisionTable();
        powerTable = fillPowerTable();
        
        int fLock = 1; // -1 = no lock, 0 = lock first coeff, 1 = lock first and second coeff
        int gLock = 0; // -1 = no lock, 0 = lock first coeff
        if(fdegree % prime == 0)
            fLock = 0;

        // String outFileName = "frac_" + prime + "_" + power + "_" + fdegree + "_" + gdegree + ".txt";
        // BufferedWriter outFile = new BufferedWriter(new FileWriter(outFileName));
        // outFile.write(GF.irr+"\r\n");
        // outFile.flush();
        
        // String permFileName = "frac_" + prime + "_" + power + "_" + fdegree + "_" + gdegree + "_perms.txt";
        // BufferedWriter permFile = null;
        // if(outputPerms)
        //     permFile = new BufferedWriter(new FileWriter(permFileName));
        
        int count = 0;
        int plusXcount = 0;
        int[] g = new int[gdegree + 1];
        Arrays.fill(g, 0);
        g[0] = 1; // g = [1, 0, ..., 0]
        do {
            int[] gValues = evaluatePolynomial(g);
            if(containsZero(gValues)) { // if g(x) produces a 0, skip it since we can't divide by zero
                continue;
            }
            int[] f = new int[fdegree + 1];
            Arrays.fill(f, 0);
            f[0] = 1; // f = [1, 0, ..., 0]
            do {
                int[] gcd = polyGCD(f, g);
                if(isOne(gcd)) {
                    boolean isPerm = false;
                    boolean plusXPerm = false;
                    int[] perm = calcPerm(f, gValues);
                    int[] plusX = new int[perm.length];
                    for(int x=0; x<plusX.length; x++)
                        plusX[x] = add(perm[x], x);
                    if(checkPerm(perm)) {
                        isPerm = true;
                        count++;
                    }
                    if(checkPerm(plusX)) {
                        plusXPerm = true;
                        plusXcount++;
                    }
                    System.out.print(Arrays.toString(f) + " / " + Arrays.toString(g) + " = " + Arrays.toString(perm));
                    if(isPerm)
                        System.out.println(" PERMUTATION");
                    else
                        System.out.println();
                    System.out.print(Arrays.toString(f) + " / " + Arrays.toString(g) + " + x = " + Arrays.toString(plusX));
                    if(plusXPerm)
                        System.out.println(" +X PERMUTATION");
                    else
                        System.out.println();
                }
            }
            while(incrementPolynomial(f, fLock));
        }
        while(incrementPolynomial(g, gLock));
        System.out.println(count + " normalized fractional permutation polynomials found");
        System.out.println(plusXcount + " +x permutations found");
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
    
    public static int power(int a, int b) { //a^b in GF
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
    
    public static boolean incrementPolynomial(int[] polynomial, int stopIndex) { // stopIndex = -1 for all indexes, 0 if poly is monic, etc.
        int curIndex = polynomial.length - 1;
        if(curIndex == stopIndex)
            return false;
        while(incrementIndex(polynomial, curIndex)) {
            curIndex--;
            if(curIndex == stopIndex)
                return false;
        }
        return true;
    }

    public static boolean incrementIndex(int[] polynomial, int index) { //return true if carries
        polynomial[index]++;
        if(polynomial[index] == GF.n) {
            polynomial[index] = 0;
            return true;
        }
        else
            return false;
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

    public static boolean checkPerm(int[] message) { // check if message is a permutation
        HashSet<Integer> values = new HashSet<>();
        for(int x=0; x<GF.n; x++) {
            if(!values.add(message[x])) // add index x to the set
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
    
    public static void print(int[] array){
        System.out.println(Arrays.toString(array));
    }
    
    public static void parseArgs(String[] args) {
        if(args.length < 2) {
            System.out.println("Usage: java FracSearch <prime> <power>");
            System.exit(0);
        }
        prime = Integer.parseInt(args[0]);
        power = Integer.parseInt(args[1]);
        fdegree = 1;
        gdegree = 2;
        GF.initGF(prime, power);
        
        // set default options
        outputPerms = false;
        
        // check additional options
        // if(args.length > 2) {
        //     for(int x=2; x<args.length; x++) {
        //         switch(args[x]) {
        //             case "-perm":
        //                 outputPerms = true;
        //                 break;
        //             default:
        //                 System.out.println("Unrecognized option "+args[x]);
        //                 System.exit(0);
        //         }
        //     }
        // }
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