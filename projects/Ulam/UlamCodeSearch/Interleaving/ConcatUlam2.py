
import itertools
import bisect
import networkx as nx

# -----------------------------
# 1. Define A
# -----------------------------
A = [
    (1, 2, 3, 4),
    (1, 4, 3, 2),
    (2, 4, 1, 3),
    (3, 2, 1, 4),
    (3, 4, 1, 2),
    (4, 2, 3, 1),
]

# -----------------------------
# 2. Define B by adding 4 to each symbol in A
# -----------------------------
B = [tuple(x + 4 for x in row) for row in A]

# -----------------------------
# 3. Build C from AB and BA
# -----------------------------
C = []

for a, b in zip(A, B):
    C.append(a + b)

for a, b in zip(A, B):
    C.append(b + a)

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
# 5. Enumerate all 8! permutations and test
# -----------------------------
symbols = list(range(1, 9))
good_new_perms = []

for perm in itertools.permutations(symbols):
    if perm in C:
        continue

    ok = True
    for q in C:
        if lcs_length(perm, q) > 4:
            ok = False
            break

    if ok:
        good_new_perms.append(perm)

# -----------------------------
# 6. Save results to a text file
# -----------------------------
filename = "valid_extensions.txt"

with open(filename, "w") as f:
    f.write("Valid permutations that can be added to C:\n")
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
# 9. Build the compatibility graph between valid extensions
# ============================================================

print("\nBuilding compatibility graph... this may take a little time.")

import networkx as nx

G = nx.Graph()

# Add nodes
for p in good_new_perms:
    G.add_node(p)

# Add edges between nodes only if LCS <= 4
# (equivalently Ulam distance >= 4)
for i, p in enumerate(good_new_perms):
    for j in range(i+1, len(good_new_perms)):
        q = good_new_perms[j]
        if lcs_length(p, q) <= 4:
            G.add_edge(p, q)

print("Graph built! Nodes:", len(G.nodes()), "Edges:", len(G.edges()))

# ============================================================
# 10. Branch-and-bound maximum clique search (faster & exact)
# ============================================================

print("\nStarting branch-and-bound max clique search...")

# Convert graph to adjacency dictionary for speed
adj = {v: set(G[v]) for v in G.nodes()}

best_clique = []

def expand(clique, candidates):
    global best_clique

    # Bound: If remaining candidates + current clique cannot beat best_clique, stop
    if len(clique) + len(candidates) <= len(best_clique):
        return

    while candidates:
        v = candidates.pop()

        new_clique = clique + [v]
        new_candidates = [u for u in candidates if u in adj[v]]

        # Expand only if possible improvement
        if len(new_clique) + len(new_candidates) > len(best_clique):
            expand(new_clique, new_candidates)

        # Update best + SAVE to file IMMEDIATELY
        if len(new_clique) > len(best_clique):
            best_clique = new_clique  # store in memory
            print(">>> New best clique size:", len(best_clique))

            # SAVE to a unique checkpoint file (different each time)
            size = len(best_clique)
            filename = f"clique_size_{size}.txt"

            with open(filename, "w") as f:
                f.write(f"Current best clique size: {size}\n\n")
                for perm in best_clique:
                    f.write(str(perm) + "\n")

            print(f"Saved new clique to {filename}")



# Start search
expand([], list(G.nodes()))

max_clique = best_clique
print("\nExact maximum clique size:", len(max_clique))
print("These permutations can all coexist in C':")
for p in max_clique:
    print(p)

# ============================================================
# 11. Save the maximum extension to a file
# ============================================================

with open("max_extension.txt", "w") as f:
    f.write("MAXIMAL EXTENSION C' (beyond C):\n")
    f.write(f"Size = {len(max_clique)} new permutations\n\n")
    for p in max_clique:
        f.write(str(p) + "\n")

print("\nMaximal extension saved to max_extension.txt")