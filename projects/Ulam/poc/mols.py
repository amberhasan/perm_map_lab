def process_latin_square(name, square_string):
    print(f"\n=== {name}: Original Matrix ===")
    
    # Parse rows
    rows = square_string.strip().split()
    
    # Print original matrix
    for row in rows:
        print(" ".join(row))
    
    # Build A encoding
    A = {}
    for r, row in enumerate(rows, start=1):
        for c, v in enumerate(row, start=1):
            A[(v, c)] = r
    
    print(f"\n=== {name}: Triplets (r,c,v) Matrix ===")
    for r, row in enumerate(rows, start=1):
        for c, v in enumerate(row, start=1):
            print(f"({r},{c},{v})", end="  ")
        print()
    
    print(f"\n=== {name}: A-Encoding Matrix (A[v,c] = r) ===")
    for r in range(1, 5):
        for c in range(1, 5):
            # FIXED: prevents KeyError
            print(A.get((str(r), c), "."), end=" ")
        print()


# -------------------------------------------------------------------
# Your Latin Squares
# -------------------------------------------------------------------

X = '''
1234
2143
3412
4321
'''

Y = '''
1423
4132
2314
3421
'''

# -------------------------------------------------------------------
# Run them
# -------------------------------------------------------------------
process_latin_square("X", X)
process_latin_square("Y", Y)
