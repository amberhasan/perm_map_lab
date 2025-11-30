import itertools
import bisect
import networkx as nx

# -----------------------------
# 1. Define A on {1,...,5}
# -----------------------------
A = [
    (1, 2, 3, 4, 5),
    (1, 5, 3, 2, 4),
    (2, 5, 1, 4, 3),
    (3, 2, 1, 5, 4),
    (3, 5, 1, 2, 4),
    (4, 2, 3, 5, 1),
]

# -----------------------------
# 2. Define B by adding 5 to each symbol in A
# -----------------------------
B = [tuple(x + 5 for x in row) for row in A]

# -----------------------------
# 3. Build C from AB and BA (length 10)
# -----------------------------
C = []

for a, b in zip(A, B):
    C.append(a + b)   # AB

for a, b in zip(A, B):
    C.append(b + a)   # BA

# -----------------------------
# 4. LCS and Ulam distance helpers
# -----------------------------
def lcs_length(p, q):
    pos_in_q = {value: idx for idx, value in enumerate(q)}
    seq = [pos_in_q[v] for v in p]

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

# -----------------------------
# 5. Enumerate ALL 10! permutations and test
# -----------------------------
symbols = list(range(1, 11))
good_new_perms = []

print("Enumerating 10! = 3,628,800 permutations...")

for perm in itertools.permutations(symbols):
    if perm in C:
        continue

    ok = True
    for q in C:
        if lcs_length(perm, q) > 4:   # must satisfy Ulam distance ≥ 4
            ok = False
            break

    if ok:
        good_new_perms.append(perm)

# -----------------------------
# 6. Save valid extensions
# -----------------------------
filename = "valid_extensions_U10_4.txt"

with open(filename, "w") as f:
    f.write("Valid permutations that can be added to C (U(10,4)):\n")
    f.write(f"Total count: {len(good_new_perms)}\n\n")
    for p in good_new_perms:
        f.write(str(p) + "\n")

print(f"Saved {len(good_new_perms)} valid permutations to {filename}")

# -----------------------------
# 8. Print C with numbering
# -----------------------------
print("\nC permutations (numbered):")
for i, row in enumerate(C, start=1):
    print(f"{i:2d}: {row}")

# ============================================================
# 9. Build compatibility graph
# ============================================================

print("\nBuilding compatibility graph...")

G = nx.Graph()

# Add nodes
for p in good_new_perms:
    G.add_node(p)

# Add edges if LCS ≤ 4 (Ulam ≥ 4)
for i, p in enumerate(good_new_perms):
    for j in range(i+1, len(good_new_perms)):
        q = good_new_perms[j]
        if lcs_length(p, q) <= 4:
            G.add_edge(p, q)

print("Graph built! Nodes:", len(G.nodes()), "Edges:", len(G.edges()))

# ============================================================
# 10. Branch-and-bound maximum clique search
# ============================================================

print("\nStarting branch-and-bound max clique search...")

adj = {v: set(G[v]) for v in G.nodes()}

best_clique = []

def expand(clique, candidates):
    global best_clique

    if len(clique) + len(candidates) <= len(best_clique):
        return

    while candidates:
        v = candidates.pop()
        new_clique = clique + [v]
        new_candidates = [u for u in candidates if u in adj[v]]

        if len(new_clique) + len(new_candidates) > len(best_clique):
            expand(new_clique, new_candidates)

        if len(new_clique) > len(best_clique):
            best_clique = new_clique
            print(">>> New best clique size:", len(best_clique))

            size = len(best_clique)
            filename = f"clique_size_{size}.txt"
            with open(filename, "w") as f:
                f.write(f"Current best clique size: {size}\n\n")
                for perm in best_clique:
                    f.write(str(perm) + "\n")

            print(f"Saved new clique to {filename}")

expand([], list(G.nodes()))

max_clique = best_clique
print("\nExact maximum clique size:", len(max_clique))
print("These permutations can all coexist in C':")
for p in max_clique:
    print(p)

with open("max_extension_U10_4.txt", "w") as f:
    f.write("MAXIMAL EXTENSION C' for U(10,4):\n")
    f.write(f"Size = {len(max_clique)} new permutations\n\n")
    for p in max_clique:
        f.write(str(p) + "\n")

print("\nMaximal extension saved to max_extension_U10_4.txt")
