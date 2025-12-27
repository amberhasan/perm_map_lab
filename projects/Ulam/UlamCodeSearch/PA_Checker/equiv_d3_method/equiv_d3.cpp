#include <iostream>   // For console input/output (cout, endl)
#include <vector>     // For dynamic arrays (vector)
#include <algorithm>  // For shuffle, lower_bound, next_permutation
#include <numeric>    // For iota (fills array with sequential values)
#include <random>     // For random number generation (mt19937, rng)
#include <chrono>     // For time measurement and seeding RNG
#include <csignal>    // For handling Ctrl+C (SIGINT)
#include <fstream>    // For file reading/writing (ifstream, ofstream)
#include <string>     // For string operations

using namespace std;

// ---------------- CONFIG ----------------
static const int D = 3;               // Minimum Ulam distance required between permutations
static const int MAX_PER_CLASS = 1;   // Max permutations to take from each equivalence class
// ----------------------------------------

// Global tracking variables
int BEST = 0;                         // Best (largest) code size found so far
vector<vector<int>> BEST_CODE;        // The permutations in the best code found
int GLOBAL_N;                         // The value of n (permutation length)

// ----------------------------------------
// Save best code to file
// ----------------------------------------
void save_best() {
    // Create filename based on n value (e.g., "U7_d3_equiv.txt" for n=7)
    string filename = "U" + to_string(GLOBAL_N) + "_d3_equiv.txt";

    ofstream out(filename);              // Open file for writing
    if (!out) return;                    // Return if file cannot be opened

    // Write header comment with best size found
    out << "# Best known U(" << GLOBAL_N << ",3) >= "
        << BEST << "\n";

    // Write each permutation in the code, one per line
    for (const auto& p : BEST_CODE) {    // For each permutation in best code
        for (int x : p)                  // For each element in permutation
            out << x << " ";             // Write element followed by space
        out << "\n";                     // Newline after each permutation
    }

    out.close();                         // Close the file
    cout << "[SAVE] Wrote " << BEST
         << " permutations to " << filename << endl;  // Notify user
}

// ----------------------------------------
// Ctrl+C handler - saves results when user interrupts program
// ----------------------------------------
void handle_sigint(int) {
    cout << "\n[STOP] Final best = " << BEST << endl;  // Display final result
    save_best();                                       // Save best code to file
    exit(0);                                           // Exit program gracefully
}

// ----------------------------------------
// Ulam distance calculation using Longest Increasing Subsequence (LIS)
// Formula: d(p,q) = n - LIS(p^{-1} ∘ q)
// where p^{-1} is the inverse of permutation p, and ∘ is composition
// ----------------------------------------
int ulam_distance(const vector<int>& p, const vector<int>& q) {
    int n = p.size();                    // Length of permutations
    vector<int> inv(n);                  // Will hold inverse of p

    // Compute inverse of permutation p: if p[i] = j, then inv[j] = i
    for (int i = 0; i < n; i++)
        inv[p[i]] = i;

    // Compute composition: seq[i] = p^{-1}(q[i])
    vector<int> seq(n);
    for (int i = 0; i < n; i++)
        seq[i] = inv[q[i]];

    // Compute LIS (Longest Increasing Subsequence) of seq using O(n log n) algorithm
    vector<int> lis;                     // lis[i] = smallest tail of increasing subseq of length i+1
    for (int x : seq) {                  // For each element in sequence
        // Binary search: find position where x should go
        auto it = lower_bound(lis.begin(), lis.end(), x);
        if (it == lis.end())             // x is larger than all elements
            lis.push_back(x);            // Extend LIS
        else
            *it = x;                     // Replace element to keep smallest tail
    }

    // Ulam distance = n - length of LIS
    return n - (int)lis.size();
}

// ----------------------------------------
// Insert two new symbols (a and b) into all possible positions of a base permutation
// This generates an equivalence class: all permutations that differ only by
// where the two new symbols are placed
// ----------------------------------------
vector<vector<int>> insert_two(const vector<int>& base, int a, int b) {
    int m = base.size();                 // Size of base permutation (n-2)
    vector<vector<int>> res;             // Will store all generated permutations

    // Try inserting 'a' at position i and 'b' at position j
    for (int i = 0; i <= m; i++) {       // Position for 'a' (can go anywhere including end)
        for (int j = 0; j <= m + 1; j++) {  // Position for 'b' (after inserting 'a', there are m+1 slots)
            if (j == i) continue;        // Skip if same position (not allowed)

            vector<int> p;               // Build new permutation
            for (int k = 0; k < m + 2; k++) {  // New permutation has length m+2
                if (k == i)
                    p.push_back(a);      // Insert 'a' at position i
                else if (k == j)
                    p.push_back(b);      // Insert 'b' at position j
                else {
                    // Insert element from base, adjusting index for insertions
                    int idx = k;
                    if (k > i) idx--;    // Adjust if past position i
                    if (k > j) idx--;    // Adjust if past position j
                    p.push_back(base[idx]);  // Add element from base permutation
                }
            }
            res.push_back(p);            // Add this permutation to results
        }
    }
    return res;                          // Return all permutations in equivalence class
}

// ----------------------------------------
// Main - Constructs permutation arrays using equivalence class method
// ----------------------------------------
int main(int argc, char* argv[]) {
    // Check command line arguments
    if (argc < 2) {
        cout << "Usage: ./equiv_d3 n\n";
        return 1;
    }

    // Parse n (permutation length) from command line
    GLOBAL_N = stoi(argv[1]);
    int n = GLOBAL_N;
    int a = n - 2;                       // Second-to-last symbol
    int b = n - 1;                       // Last symbol

    // Register Ctrl+C handler to save results when interrupted
    signal(SIGINT, handle_sigint);

    // Initialize random number generator with current time as seed
    mt19937 rng(chrono::high_resolution_clock::now()
                    .time_since_epoch().count());

    long long ITER = 0;                  // Count iterations

    // Main loop: repeatedly try different orderings of equivalence classes
    while (true) {
        ITER++;                          // Increment iteration counter

        vector<vector<int>> code;        // Current permutation array being built

        // Create base permutation of size n-2: [0, 1, 2, ..., n-3]
        vector<int> base(n - 2);
        iota(base.begin(), base.end(), 0);

        // Iterate through all permutations of the base (there are (n-2)! of them)
        do {
            // Generate equivalence class by inserting symbols a and b in all positions
            auto eq = insert_two(base, a, b);

            // Randomly shuffle the equivalence class (changes selection order)
            shuffle(eq.begin(), eq.end(), rng);

            int taken = 0;               // Count permutations taken from this class
            // Try to add permutations from this equivalence class
            for (const auto& p : eq) {
                bool ok = true;          // Assume p can be added

                // Check if p has sufficient distance from all permutations already in code
                for (const auto& q : code) {
                    if (ulam_distance(p, q) < D) {  // Distance too small
                        ok = false;      // Cannot add p
                        break;           // Stop checking
                    }
                }

                // If p is far enough from all existing permutations, add it
                if (ok) {
                    code.push_back(p);   // Add p to code
                    taken++;             // Increment count from this class
                    if (taken >= MAX_PER_CLASS)  // Stop after taking enough from this class
                        break;
                }
            }

        } while (next_permutation(base.begin(), base.end()));  // Try next base permutation

        int sz = (int)code.size();       // Size of code constructed this iteration

        // Check if this is the best code found so far
        if (sz > BEST) {
            BEST = sz;                   // Update best size
            BEST_CODE = code;            // Save best code
            cout << "[NEW BEST] U(" << n << ",3) ≥ "
                 << BEST << "   (iter " << ITER << ")" << endl;
            save_best();                 // Write to file
        }

        // Periodic progress update every 50 iterations
        if (ITER % 50 == 0) {
            cout << "[iter " << ITER
                 << "] current=" << sz
                 << "  best=" << BEST << endl;
        }
    }

    return 0;
}
