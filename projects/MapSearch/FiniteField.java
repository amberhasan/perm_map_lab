import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class FiniteField {
	static String newLine = System.getProperty("line.separator");
	static int[] coef = null;
	public static void main(String [] args) {
		int p,r,k;
		if(args.length >= 3) {
			p = Integer.parseInt(args[0]);
			r = Integer.parseInt(args[1]);
			k = Integer.parseInt(args[2]);
			if(args.length > 3) {
				coef = new int[r+1];
				for(int i = 3; i < args.length; i++) {
					if(args[i].matches("\\d+"))
						coef[coef.length - i + 2] = Integer.parseInt(args[i]);
					else {
						System.out.println("Expected number. Instead given input: " + args[i]);
						return;
					}
				}
			}
		}
		else {
			System.out.println("Too few arguments. Usage: java FiniteField <p> <r> <k> [{coefs}] where p-prime, r-exponent, k-number of AGL cosets to display. Additionally, any number of arguments following <k> will be taken as coefficients for the Primitive Polynomial.");
			return;
		}
		int[][][] cos = AGLCosets(p,r);
		printCosets(cos, k);
	}
	
	
	public static void printCosets(int[][][] blocks, int k) {
		for(int i = 0; i < Math.min(blocks.length, k); i++) {
			System.out.printf(newLine + "Coset %d:" + newLine, i);
			print(blocks[i]);
		}
	}
	
	
	public static int[][][] AGLCosets(int p, int r) {
		int[][] agl = AGL(p,r);
		int n = (int)Math.pow(p, r);
		int[][][] res = new int[n-1][n][];
		int block = 0;
		int row = 0;
		for(int perm = 0; perm < agl.length; perm++) {
			res[block][row++] = agl[perm];
			if(row == n) {
				block++;
				row = 0;
			}
		}
		return res;
	}
	
	public static void print(int[][] arr) {
		for(int i = 0; i < arr.length; i++) {
			for(int j = 0; j < arr[i].length; j++) {
				System.out.printf("%4d", arr[i][j]);
			}
			System.out.println();
		}
	}
	
	public static int[][] AGL(int p, int r) {
		int n = (int)Math.pow(p, r);
		int[][] pArr = new int[n*(n-1)][n];
		int[][][] tables = GF(p,r);
		int k = 0;
		for(int i = 1; i < n; i++)
			for(int j = 0; j < n; j++)
				pArr[k++] = evaluateAGL(tables, i, j);
		return pArr;
	}
	
	public static int[] evaluateAGL(int[][][] tables, int a, int b) {
		int[] res = new int[tables[0].length];
		for(int i = 0; i < res.length; i++)
			res[i] = tables[0][tables[1][a][i]][b];
		return res;
	}
	
	public static int[][][] GF(int prime, int power) {
		if(!(new BigInteger(Integer.toString(prime))).isProbablePrime(10000)) {
			System.out.println("Not a prime.");
			System.exit(1);
		}
		Polynomial.mod = prime;
		
		Polynomial irr;
		if(coef == null)
			irr = findRandomPrimitive(prime, power);
		else {
			irr = new Polynomial(coef);
			if(isReducible(irr, prime)) {
				System.out.println(irr + " is a reducible polynomial.");
				System.exit(1);
			}
			if(!isPrimitive(irr,(int)Math.pow(prime, power))) {
				System.out.println(irr + " is not a primitive polynomial.");
				System.exit(1);
			}
		}
		int[][][] res = new int[3][][];
		List<Polynomial> arr = genPolynomials(prime, power, irr);
		System.out.println("Primitive Polynomial: " + irr + newLine);
		System.out.println("[0] 0 = 0");
		for(int i = 1; i < arr.size(); i++) {
			Polynomial p = arr.get(i);
			System.out.printf("[%d] x^%d = %s%n", i, i-1, p.toString());
		}
		res[0] = addTable(prime, power, arr);
		res[1] = multTable(prime, power, irr, arr);
		res[2] = new int[1][];
		return res;
	}
	
	public static int[][] multTable(int prime, int pow, Polynomial irr, List<Polynomial> arr) {
		Polynomial.mod = prime;
		int sz = (int)Math.pow(prime, pow);
		int[][] table = new int[sz][sz];
		for(int a = 0; a < sz; a++)
			for(int b = 0; b < sz; b++) {
				Polynomial result = arr.get(a).mult(arr.get(b)).divide(irr)[1];
				for(int i = 0; i < arr.size(); i++)
					if(arr.get(i).equals(result)) {
						table[a][b] = i;
						break;
					}
			}
		return table;
	}
	
	public static int[][] addTable(int prime, int pow, List<Polynomial> arr) {
		Polynomial.mod = prime;
		int sz = (int) Math.pow(prime, pow);
		int[][] table = new int[sz][sz];
		for(int a = 0; a < sz; a++)
			for(int b = 0; b < sz; b++) {
				Polynomial result = arr.get(a).add(arr.get(b));
				for(int i = 0; i < arr.size(); i++) {
					if(arr.get(i).equals(result)) {
						table[a][b] = i;
						break;
					}
				}
			}
		return table;
	}
	
	
	
//	public static ArrayList<Polynomial> genPolynomials(int mod, int deg) {
//		ArrayList<Polynomial> res = new ArrayList<Polynomial>();
//		int[] coef = new int[deg];
//		recurse(coef, 0, res, mod);
//		for(int i = 1; i < res.size(); i++)
//			if(res.get(i).deg == 0 && res.get(i).coef[0] == 1) {
//				Polynomial temp = res.get(1);
//				res.set(1, res.get(i));
//				res.set(i, temp);
//				break;
//			}
//		return res;
//	}
	
	public static List<Polynomial> genPolynomials(int prime, int power, Polynomial irr) {
		int n = (int)Math.pow(prime, power);
		Polynomial base = Polynomial.monomial(0);
		Polynomial x = Polynomial.monomial(1);
		Polynomial zero = Polynomial.zero();
		List<Polynomial> arr = new ArrayList<Polynomial>();
		arr.add(zero);
		do {
			arr.add(base.divide(irr)[1]);
			base = base.mult(x);
		}
		while(arr.size() < n);
		return arr;
	}
	
	public static Polynomial findRandomPrimitive(int prime, int power) {
		int n = (int)Math.pow(prime, power);
		int[] temp = new int[n + 1];
		temp[temp.length-1] = 1;
		temp[1] = prime - 1;
		Polynomial cur = randomPolynomial(prime, power);
		while(isReducible(cur, prime) || !isPrimitive(cur, n))
			cur = randomPolynomial(prime, power);
		return cur;
	}
	
	public static boolean isPrimitive(Polynomial p, int n) {
		int d = n - 1;
		if(p.equals(Polynomial.monomial(1)))
			return false;
		for(int x = 1; x <= d-1; x++)
			if(d % x == 0 && Polynomial.monomial(x).divide(p)[1].isOne())
				return false;
		return true;
	}
	
	public static boolean isReducible(Polynomial p, int mod) {
		for(int i = 1; i < p.deg; i++) {
			int[] coef = new int[(int)Math.pow(mod, i)+1];
			coef[coef.length-1] = 1;
			coef[1] = mod-1;
			Polynomial test = new Polynomial(coef);
			if(Polynomial.gcd(p, test).deg > 0) {
				return true;
			}
		}
		return false;
	}
	
	public static Polynomial randomPolynomial(int mod, int deg) {
		int[] coef = new int[deg+1];
		Random rand = new Random();
		coef[coef.length-1] = 1;
		for(int i = 0; i < coef.length-1; i++)
			coef[i] = rand.nextInt(mod);
		return new Polynomial(coef);
	}
}

class Polynomial {
	static int mod = 0;
	int[] coef;
	int deg;
	
	public Polynomial(int[] coef) {
		deg = coef.length-1;
		while(deg >= 0 && coef[deg] == 0)
			deg--;
		this.coef = Arrays.copyOf(coef, deg+1);
	}
	
	public static Polynomial zero() {
		int[] coef = new int[1];
		coef[0] = 0;
		return new Polynomial(coef);
	}
	
	public static Polynomial monomial(int n) {
		int[] coef = new int[n+1];
		coef[n] = 1;
		return new Polynomial(coef);
	}
	
	public boolean isOne() {
		return deg == 0 && coef[0] == 1;
	}
	
	public Polynomial mult(Polynomial other) {
		if(deg == -1 || other.deg == -1)
			return new Polynomial(new int[]{});
		int[] newCoef = new int[deg + other.deg + 1];
		for(int i = 0; i < coef.length; i++)
			for(int j = 0; j < other.coef.length; j++)
				newCoef[i+j] = (coef[i]*other.coef[j] + newCoef[i+j]) % mod;
		return new Polynomial(newCoef);
	}
	
	public Polynomial add(Polynomial other) {
		int[] newCoef = new int[Math.max(coef.length, other.coef.length)];
		for(int i = 0; i < newCoef.length; i++) {
			if(i < coef.length)
				newCoef[i] = (newCoef[i] + coef[i]) % mod;
			if(i < other.coef.length)
				newCoef[i] = (newCoef[i] + other.coef[i]) % mod;
		}
		return new Polynomial(newCoef);
	}
	
	public Polynomial subtract(Polynomial other) {
		int[] newCoef = new int[Math.max(coef.length, other.coef.length)];
		for(int i = 0; i < newCoef.length; i++) {
			if(i < coef.length)
				newCoef[i] = (coef[i]+ newCoef[i]) % mod;
			if(i < other.coef.length)
				newCoef[i] = (newCoef[i] - other.coef[i] + mod) % mod;
		}
		return new Polynomial(newCoef);
	}
	
	public Polynomial divideLeadTerms(Polynomial other) {
		int temp = deg - other.deg;
		int value = coef[coef.length-1] * invert(other.coef[other.coef.length-1]);
		int newCoef[] = new int[temp+1];
		newCoef[temp] = value;
		return new Polynomial(newCoef);
	}
	
	public Polynomial[] divide(Polynomial divisor) {
		Polynomial[] res = new Polynomial[2];
		if(divisor.deg == -1)
			return null;
		res[0] = new Polynomial(new int[]{});
		res[1] = this.copy();
		while(res[1].deg != -1 && res[1].deg >= divisor.deg) {
			Polynomial temp = res[1].divideLeadTerms(divisor);
			res[0] = res[0].add(temp);
			res[1] = res[1].subtract(temp.mult(divisor));
		}
		return res;
	}
	
	public static int invert(int n) {
		for(int a = 1; a < mod; a++)
			if((a * n) % mod == 1)
				return a;
		return -1;
	}
	
	public static Polynomial gcd(Polynomial a, Polynomial b) {
		if(b.deg == -1)
			return a;
		else
			return gcd(b, a.divide(b)[1]);
	}
	
	public Polynomial copy() {
		int[] newCoef = Arrays.copyOf(coef, coef.length);
		return new Polynomial(newCoef);
	}
	
	public boolean equals(Object other) {
		if(other instanceof Polynomial) {
			Polynomial poly = (Polynomial)other;
			if(poly.deg != deg)
				return false;
			for(int i = 0; i < coef.length; i++)
				if(coef[i] != poly.coef[i])
					return false;
			return true;
		}
		return false;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(deg == -1)
			return "0";
		for(int i = coef.length-1; i >= 2; i--) {
			if(coef[i] == 1)
				sb.append("x^" + i + " + ");
			else if(coef[i] != 0)
				sb.append(coef[i] + "x^" + i + " + ");
		}
		if(coef.length >= 2 && coef[1] != 0)
			sb.append((coef[1] == 1 ? "" : coef[1]) + "x + ");
		if(coef.length >= 1 && coef[0] != 0)
			sb.append(coef[0]);
		else
			sb.delete(sb.length()-3, sb.length());
		return sb.toString();
	}
}