import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class GaloisCalc
{
	/**
	 * 
	 * @param args
	 *            args[0] - Prime args[1] - Power args[2] - Primitive Polynomial
	 *            in the form ax^b+cx^d+...+ex^1+f
	 */
	public static void main(String[] args)
	{
		//args = new String[] { "3", "4", "x^4+2x^3+2" };

		if (args.length != 3 && args.length != 2)
		{
			System.out.println("Incorrect usage.");
			System.out.println("java GaloisCalc <prime> <power> [primitivePolynomial]");
			System.out.println("Primitive Polynomial of the form ax^b+cx^d+...+ex^1+f");
			return;
		}

		int prime = Integer.parseInt(args[0]);
		int power = Integer.parseInt(args[1]);

		if (args.length == 3)
		{
			ArrayList<Integer> poly = new ArrayList<>();
			poly.add(0);
			poly.add(0);

			String[] primString = args[2].split("\\+");
			for (String s : primString)
			{
				String[] parts = s.split("x\\^");

				if (parts.length == 1)
				{
					if (parts[0].endsWith("x"))
					{
						String val = parts[0].substring(0, parts[0].length() - 1);
						if (val.equals(""))
							poly.set(1, 1);
						else
							poly.set(1, Integer.parseInt(val));
					}
					else
					{
						// Assume constant
						poly.set(0, Integer.parseInt(parts[0]));
					}
				}
				else
				{
					int arrayIndex = Integer.parseInt(parts[1]);

					int value = 0;
					if (parts[0].equals(""))
						value = 1;
					else
						value = Integer.parseInt(parts[0]);

					if (poly.size() >= arrayIndex)
						poly.set(arrayIndex, value);
					else
					{
						while (poly.size() <= arrayIndex)
							poly.add(0);
						poly.set(arrayIndex, value);
					}
				}
			}

			int[] polyArray = new int[poly.size()];
			for (int i = 0; i < polyArray.length; i++)
				polyArray[i] = poly.get(i);
			GF.irr = new Polynomial(polyArray);
		}

		long startT = System.currentTimeMillis();
		Polynomial res = GF.initGF(prime, power);
		long endT = System.currentTimeMillis();
		System.out.println(res);

		System.out.println("Took: " + (endT - startT) + "ms");

		Scanner scanner = new Scanner(System.in);

		String lastLine = "";

		while (true)
		{
			String lineIn = scanner.nextLine();

			if (lineIn.equals(""))
				lineIn = lastLine;
			else
				lastLine = lineIn;

			try
			{
				if (lineIn.equals("exit"))
					break;
				if (lineIn.contains("+"))
				{
					String[] parts = lineIn.split("\\+");

					if (parts[0].contains("["))
					{
						String[] subParts = parts[0].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
						int[] a = new int[subParts.length];
						for (int i = 0; i < subParts.length; i++)
							a[i] = Integer.parseInt(subParts[i].trim());

						subParts = parts[1].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
						int[] b = new int[subParts.length];
						for (int i = 0; i < subParts.length; i++)
							b[i] = Integer.parseInt(subParts[i].trim());

						// [2, 2, 3] * [4, 5, 6, 7]
						// (2x^2 + 2x + 3) * (4x^3 + 5x^2 + 6x + 7)
						int[] out = GFAdd(a, b);
						System.out.println(Arrays.toString(out));
					}
					else
					{
						int a = Integer.parseInt(parts[0].trim());
						int b = Integer.parseInt(parts[1].trim());
						System.out.println(GFAdd(a, b));
					}
				}
				else if (lineIn.contains("-"))
				{
					String[] parts = lineIn.split("\\-");

					if (parts[0].contains("["))
					{
						String[] subParts = parts[0].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
						int[] a = new int[subParts.length];
						for (int i = 0; i < subParts.length; i++)
							a[i] = Integer.parseInt(subParts[i].trim());

						subParts = parts[1].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
						int[] b = new int[subParts.length];
						for (int i = 0; i < subParts.length; i++)
							b[i] = Integer.parseInt(subParts[i].trim());

						// [2, 2, 3] * [4, 5, 6, 7]
						// (2x^2 + 2x + 3) * (4x^3 + 5x^2 + 6x + 7)
						int[] out = GFSub(a, b);
						System.out.println(Arrays.toString(out));
					}
					else
					{
						int a = Integer.parseInt(parts[0].trim());
						int b = Integer.parseInt(parts[1].trim());

						for (int i = 0; i < GF.n; i++)
						{
							if (GFAdd(b, i) == a)
							{
								System.out.println(i);
							}
						}
					}
				}
				else if (lineIn.contains("*"))
				{
					String[] parts = lineIn.split("\\*");

					if (parts[0].contains("["))
					{
						String[] subParts = parts[0].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
						int[] a = new int[subParts.length];
						for (int i = 0; i < subParts.length; i++)
							a[i] = Integer.parseInt(subParts[i].trim());

						subParts = parts[1].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
						int[] b = new int[subParts.length];
						for (int i = 0; i < subParts.length; i++)
							b[i] = Integer.parseInt(subParts[i].trim());

						// [2, 2, 3] * [4, 5, 6, 7]
						// (2x^2 + 2x + 3) * (4x^3 + 5x^2 + 6x + 7)
						int[] out = GFMul(a, b);
						System.out.println(Arrays.toString(out));
					}
					else
					{
						int a = Integer.parseInt(parts[0].trim());
						int b = Integer.parseInt(parts[1].trim());
						System.out.println(GFMul(a, b));
					}
				}
				else if (lineIn.contains("/"))
				{
					String[] parts = lineIn.split("\\/");

					if (parts[0].contains("["))
					{
						String[] subParts = parts[0].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
						int[] dividen = new int[subParts.length];
						for (int i = 0; i < subParts.length; i++)
							dividen[i] = Integer.parseInt(subParts[i].trim());

						subParts = parts[1].replaceAll("\\[", "").replaceAll("\\]", "").split(",");
						int[] divisor = new int[subParts.length];
						for (int i = 0; i < subParts.length; i++)
							divisor[i] = Integer.parseInt(subParts[i].trim());

						// [2, 2, 3] * [4, 5, 6, 7]
						// (2x^2 + 2x + 3) * (4x^3 + 5x^2 + 6x + 7)
						int[] out = new int[1];

						// System.out.println("---------------------------");

						while (dividen.length - 1 >= divisor.length - 1 && !(dividen.length == 1 && dividen[0] == 0))
						{
							int deltaDegree = (dividen.length - 1) - (divisor.length - 1);

							// System.out.println("Dividen: " +
							// Arrays.toString(dividen));
							// System.out.println("Divisor: " +
							// Arrays.toString(divisor));
							// System.out.println("Res: " +
							// Arrays.toString(out));
							// System.out.println(deltaDegree);

							int a = dividen[0];
							int b = divisor[0];

							int mul = 1;
							for (int i = 0; i < GF.n; i++)
								if (a == GFMul(b, i))
								{
									mul = i;
									break;
								}

							// System.out.println(a + " * " + mul + " = " + b);

							assert (mul != -1);

							int[] tmp = new int[deltaDegree + 1];
							tmp[0] = mul;

							// System.out.println("Poly: " +
							// Arrays.toString(tmp));

							out = GFAdd(out, tmp);
							tmp = GFMul(tmp, divisor);

							dividen = GFSub(dividen, tmp);

							// System.out.println("---------------------------");

						}

						System.out.println(Arrays.toString(out));
						System.out.println("R: " + Arrays.toString(dividen));
					}
					else
					{
						int a = Integer.parseInt(parts[0].trim());
						int b = Integer.parseInt(parts[1].trim());

						for (int i = 0; i < GF.n; i++)
						{
							if (GFMul(b, i) == a)
							{
								System.out.println(i);
							}
						}
					}
				}
				else if (lineIn.contains("sa"))
				{
					String[] parts = lineIn.split("sa");
					int a = Integer.parseInt(parts[0].trim());
					int b = Integer.parseInt(parts[1].trim());

					for (int i = 0; i < GF.n; i++)
					{
						if (GFAdd(a, i) == b)
						{
							System.out.println(a + " + " + i + " = " + b);
						}
					}
				}
				else if (lineIn.contains("sm"))
				{
					String[] parts = lineIn.split("sm");
					int a = Integer.parseInt(parts[0].trim());
					int b = Integer.parseInt(parts[1].trim());

					for (int i = 0; i < GF.n; i++)
					{
						if (GFMul(a, i) == b)
						{
							System.out.println(a + " * " + i + " = " + b);
						}
					}
				}
				else if (lineIn.contains("orbitAdd"))
				{
					String[] parts = lineIn.split(" ");
					int start = Integer.parseInt(parts[1].trim());
					int add = Integer.parseInt(parts[2].trim());

					int cur = start;

					int size = 0;

					do
					{
						size++;
						System.out.println(cur);
						cur = GFAdd(cur, add);
					}
					while (cur != start);

					System.out.println("Found orbit of size " + size);
				}
				else if (lineIn.contains("orbitMul"))
				{
					String[] parts = lineIn.split(" ");
					int start = Integer.parseInt(parts[1].trim());
					int mul = Integer.parseInt(parts[2].trim());

					int cur = start;

					int size = 0;

					do
					{
						size++;
						System.out.println(cur);
						cur = GFMul(cur, mul);
					}
					while (cur != start);

					System.out.println("Found orbit of size " + size);
				}
				else if (lineIn.contains("atable"))
				{
					System.out.print(" \t");
					for (int i = 0; i < GF.n; i++)
						System.out.print(i + "\t");
					System.out.println();
					for (int i = 0; i < GF.n; i++)
					{
						System.out.print(i + "\t");
						for (int j = 0; j < GF.n; j++)
							System.out.print(GFAdd(i, j) + "\t");
						System.out.println();
					}
				}
				else if (lineIn.contains("test"))
				{
					String curLine = null;
					BufferedReader in = new BufferedReader(new FileReader(new File("/Users/tommy__123__/Desktop/Permutation Research/gpsRS/OrbitSearchV4/2^6_5.txt")));

					ArrayList<int[]> masters = new ArrayList<>();
					ArrayList<Boolean> mastersTF = new ArrayList<>();

					while ((curLine = in.readLine()) != null)
					{
						if (!curLine.startsWith("["))
							continue;

						int[] cur = Arrays.stream(curLine.substring(1, curLine.length() - 1).split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
						masters.add(cur);
						mastersTF.add(false);
					}

					in.close();

					while (true)
					{
						lineIn = scanner.nextLine();

						if (lineIn.equals("print"))
						{
							int hitCount = 0;
							for (int i = 0; i < masters.size(); i++)
							{
								if (mastersTF.get(i))
									hitCount++;
								System.out.println(Arrays.toString(masters.get(i)) + " " + mastersTF.get(i));
							}

							System.out.println(hitCount + "/" + masters.size());
							continue;
						}

						int a = Integer.parseInt(lineIn.split(" ")[0]);
						int b = Integer.parseInt(lineIn.split(" ")[1]);
						int mulA = Integer.parseInt(lineIn.split(" ")[2]);
						int mulB = Integer.parseInt(lineIn.split(" ")[3]);

						int hit = 0;
						int attempt = 0;
						int dupe = 0;

						int startA = a;
						int startB = b;

						do
						{
							// boolean hasHit = false;
							for (int i = 0; i < masters.size(); i++)
							{
								if (masters.get(i)[2] == a && masters.get(i)[3] == b)
								{
									hit++;

									if (mastersTF.get(i))
										dupe++;

									mastersTF.set(i, true);

									// hasHit = true;

									break;
								}
							}

							// System.out.println(a + "\t" + b + "\t-\t" +
							// hasHit);

							attempt++;

							a = GFMul(a, mulA);
							b = GFMul(b, mulB);
						}
						while (startA != a || startB != b);

						System.out.println(lineIn + " Hit/Attempt: " + hit + "/" + attempt + " - dupe: " + dupe);
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		scanner.close();
	}

	static int GFAdd(int a, int b)
	{
		return GF.addTable[a + b * GF.n];
	}

	static int GFSub(int a, int b)
	{
		for (int i = 0; i < GF.n; i++)
		{
			if (GFAdd(b, i) == a)
			{
				return i;
			}
		}

		throw new RuntimeException("Error: No valid subtraction");
	}

	static int GFMul(int a, int b)
	{
		return GF.mulTable[a + b * GF.n];
	}

	static int[] GFAdd(int a[], int b[])
	{
		int[] res = new int[Math.max(a.length, b.length)];

		if (a.length == b.length)
			for (int i = 0; i < a.length; i++)
				res[i] = GFAdd(a[i], b[i]);
		else if (a.length < b.length)
		{
			int delta = b.length - a.length;

			for (int i = 0; i < delta; i++)
				res[i] = b[i];
			for (int i = delta; i < b.length; i++)
				res[i] = GFAdd(b[i], a[i - delta]);
		}
		else
		{
			int delta = a.length - b.length;

			for (int i = 0; i < delta; i++)
				res[i] = a[i];
			for (int i = delta; i < a.length; i++)
				res[i] = GFAdd(a[i], b[i - delta]);
		}

		return res;
	}

	static int[] GFSub(int a[], int b[])
	{
		int[] res = new int[Math.max(a.length, b.length)];

		if (a.length == b.length)
			for (int i = 0; i < a.length; i++)
				res[i] = GFSub(a[i], b[i]);
		else if (a.length < b.length)
		{
			int delta = b.length - a.length;

			for (int i = 0; i < delta; i++)
				res[i] = GFSub(0, b[i]);
			for (int i = delta; i < b.length; i++)
				res[i] = GFSub(a[i - delta], b[i]);
		}
		else
		{
			int delta = a.length - b.length;

			for (int i = 0; i < delta; i++)
				res[i] = a[i];
			for (int i = delta; i < a.length; i++)
				res[i] = GFSub(a[i], b[i - delta]);
		}

		int numLeading = 0;
		for (int i = 0; i < res.length; i++)
			if (res[i] == 0)
				numLeading++;
			else
				break;

		if (res.length == numLeading)
		{
			int[] finalRes = new int[1];
			return finalRes;
		}

		int newLength = Math.max(res.length - numLeading, 1);
		int[] finalRes = new int[newLength];
		for (int i = 0; i < newLength; i++)
		{
			finalRes[i] = res[i + numLeading];
		}

		return finalRes;
	}

	static int[] GFMul(int[] a, int[] b)
	{
		int[] out = new int[a.length - 1 + b.length - 1 + 1];

		for (int i = 0; i < a.length; i++)
			for (int j = 0; j < b.length; j++)
			{
				int index = out.length - 1 - ((a.length - 1 - i) + (b.length - 1 - j));
				out[index] = GFAdd(out[index], GFMul(a[i], b[j]));
			}

		return out;
	}
}

class GF
{
	static Random rand = new Random();

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

		long startT = System.currentTimeMillis();
		if (irr == null)
			irr = findRandomPrimitive(prime, power);
		long endT = System.currentTimeMillis();

		System.out.println("Found primitive (" + (endT - startT) + "ms)");

		startT = System.currentTimeMillis();
		List<Polynomial> arr = genPolynomials(prime, power, irr);
		endT = System.currentTimeMillis();
		System.out.println("Found polys (" + (endT - startT) + "ms)");

		startT = System.currentTimeMillis();
		addTable = addTable(prime, power, arr);
		mulTable = multTable(prime, power, irr, arr);
		endT = System.currentTimeMillis();
		System.out.println("Found tables (" + (endT - startT) + "ms)");

		return irr;
	}

	public static Polynomial findRandomPrimitive(int prime, int power)
	{
		int n = (int) Math.pow(prime, power);

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
