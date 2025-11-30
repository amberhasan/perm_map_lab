import itertools
import bisect

# =============================
# 1. Define A and build C
# =============================

A = [
    (1, 2, 3, 4),
    (1, 4, 3, 2),
    (2, 4, 1, 3),
    (3, 2, 1, 4),
    (3, 4, 1, 2),
    (4, 2, 3, 1),
]

# B by adding 4 to each symbol in A
B = [tuple(x + 4 for x in row) for row in A]

# Build C = AB ∪ BA
C = []
for a, b in zip(A, B):
    C.append(a + b)
for a, b in zip(A, B):
    C.append(b + a)

C_set = set(C)  # for O(1) membership

# =============================
# 2. LCS / Ulam helpers (optimized)
# =============================

def lcs_length(p, q):
    # p, q are 8-tuples of symbols 1..8
    pos_in_q = [0] * 9  # 1-based symbols up to 8
    for idx, v in enumerate(q):
        pos_in_q[v] = idx

    # Build index sequence
    seq = [pos_in_q[v] for v in p]

    # LIS via patience sorting (tails)
    tails = []
    for x in seq:
        i = bisect.bisect_left(tails, x)
        if i == len(tails):
            tails.append(x)
        else:
            tails[i] = x
    return len(tails)

def ulam_distance(p, q):
    return len(p) - lcs_length(p, q)

# =============================
# 3. Enumerate all 8! perms & filter valid extensions
# =============================

symbols = [1, 2, 3, 4, 5, 6, 7, 8]
good_new_perms = []

print("Enumerating 8! permutations and testing against C...")

for perm in itertools.permutations(symbols):
    if perm in C_set:
        continue

    ok = True
    for q in C:
        # Only accept if LCS <= 4 (Ulam distance >= 4)
        if lcs_length(perm, q) > 4:
            ok = False
            break

    if ok:
        good_new_perms.append(perm)

print("Valid new permutations (|good_new_perms|):", len(good_new_perms))

# Save list for inspection
with open("valid_extensions.txt", "w") as f:
    f.write("Valid permutations that can be added to C:\n")
    f.write(f"Total count: {len(good_new_perms)}\n\n")
    for p in good_new_perms:
        f.write(str(p) + "\n")

# =============================
# 4. Build compatibility graph using bitsets
# =============================

print("Building compatibility graph on valid extensions...")

n = len(good_new_perms)
# adjacency as list of int bitmasks: adj[i] bit j = 1 if edge between i and j
adj = [0] * n

for i in range(n):
    pi = good_new_perms[i]
    ai = adj[i]
    for j in range(i + 1, n):
        pj = good_new_perms[j]
        if lcs_length(pi, pj) <= 4:  # edge if compatible (Ulam distance >= 4)
            ai |= (1 << j)
            adj[j] |= (1 << i)
    adj[i] = ai

edge_count = 0
for i in range(n):
    # count each edge once (only j > i, but now we count all and divide by 2)
    edge_count += adj[i].bit_count()
edge_count //= 2

print(f"Graph built: |V| = {n}, |E| = {edge_count}")

# =============================
# 5. Bitset Bron–Kerbosch with pivot + branch & bound
# =============================

print("Starting Bron–Kerbosch max clique search (bitset version)...")

best_clique_bits = 0
best_size = 0

# Precompute a heuristic node ordering for initial P:
# sort nodes by degree descending (can help prune faster)
nodes_by_deg = sorted(range(n), key=lambda i: adj[i].bit_count(), reverse=True)
P0 = 0
for i in nodes_by_deg:
    P0 |= (1 << i)
R0 = 0
X0 = 0

def bronk(R, P, X):
    # R, P, X are ints (bitsets)
    global best_clique_bits, best_size, adj

    # Branch-and-bound: if |R| + |P| <= best_size, no hope
    size_R = R.bit_count()
    if size_R + P.bit_count() <= best_size:
        return

    if P == 0 and X == 0:
        # maximal clique
        if size_R > best_size:
            best_size = size_R
            best_clique_bits = R
        return

    # Choose pivot u from P ∪ X with max |P ∩ N(u)|
    # This is standard Bron–Kerbosch with pivoting
    UX = P | X
    # if UX == 0, no pivot; explore all P directly
    if UX != 0:
        max_deg = -1
        u = -1
        tmp = UX
        while tmp:
            lsb = tmp & -tmp
            idx = lsb.bit_length() - 1
            deg = (adj[idx] & P).bit_count()
            if deg > max_deg:
                max_deg = deg
                u = idx
            tmp ^= lsb
        # P \ N(u)
        candidates = P & ~adj[u]
    else:
        candidates = P

    # Explore each candidate vertex v
    while candidates:
        lsb = candidates & -candidates
        v = lsb.bit_length() - 1
        candidates ^= lsb

        Nv = adj[v]
        bronk(R | (1 << v), P & Nv, X & Nv)

        P ^= (1 << v)
        X |= (1 << v)
        # Additional bound check after pruning
        if R.bit_count() + P.bit_count() <= best_size:
            return

# Run the search
bronk(R0, P0, X0)

print("\nExact maximum clique size:", best_size)

# Decode clique bits to actual permutations
max_clique_perms = []
tmp = best_clique_bits
idx = 0
while tmp:
    lsb = tmp & -tmp
    i = lsb.bit_length() - 1
    max_clique_perms.append(good_new_perms[i])
    tmp ^= lsb

# =============================
# 6. Save the maximum extension to a file
# =============================

with open("max_extension.txt", "w") as f:
    f.write("MAXIMAL EXTENSION C' (beyond C):\n")
    f.write(f"Size = {len(max_clique_perms)} new permutations\n\n")
    for p in max_clique_perms:
        f.write(str(p) + "\n")

print("These permutations can all coexist in C':")
for p in max_clique_perms:
    print(p)

print("\nMaximal extension saved to max_extension.txt")
