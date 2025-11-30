from sage.all import *

# Generate MOLS for n=4
MOLS_table(4)

# Generate MOLS for n=5 and compare with the reference table
MOLS_table(5, compare=True) 

# from math import gcd

# n=4
# for a in range(n)
#     for b in range(n)
#         for x in range(n):
#                 print(a*x + b, end = "")
#         print()
#     print()
#     print('---')