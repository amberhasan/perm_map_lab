#include <algorithm>   // For sorting, shuffling, lower_bound
#include <atomic>      // For atomic variables (thread-safe STOP flag)
#include <chrono>      // For timing and performance measurement
#include <csignal>     // For handling Ctrl+C signal (SIGINT)
#include <fstream>     // For file input/output (reading/writing codes)
#include <iostream>    // For console output
#include <mutex>       // For thread-safe access to shared variables
#include <numeric>     // For iota (fill array with sequential values)
#include <random>      // For random number generation (shuffling)
#include <sstream>     // For string stream parsing
#include <string>      // For string operations
#include <vector>      // For dynamic arrays

using namespace std;

// Global configuration variables
static int GLOBAL_N = 0;                          // Permutation length
static int GLOBAL_D = 0;                          // Minimum required Ulam distance

// Thread control
static atomic<bool> STOP(false);                  // Flag to stop computation (set by Ctrl+C)

// Best solution tracking (shared between threads, protected by mutex)
static mutex best_mtx;                            // Mutex to protect best_code and best_inv
static vector<vector<int>> best_code;             // Best permutation array found so far
static vector<vector<int>> best_inv;              // Inverses of best_code permutations
static chrono::steady_clock::time_point best_last_save;  // Last time best was saved

// -------------------------
// Helpers: I/O - Functions for file naming and reading/writing codes
// -------------------------

// Generate filename for best solution (e.g., "best_U7_3.txt")
static string best_filename(int n, int d) {
    return "best_U" + to_string(n) + "_" + to_string(d) + ".txt";
}

// Generate filename for current solution (e.g., "current_U7_3.txt")
static string current_filename(int n, int d) {
    return "current_U" + to_string(n) + "_" + to_string(d) + ".txt";
}

// Save a permutation array to file
static void save_code(const string& filename, int n, int d, const vector<vector<int>>& code) {
    ofstream out(filename);              // Open file for writing
    if (!out) return;                    // Return if cannot open file

    // Write header comment with parameters and size
    out << "# U(" << n << "," << d << ") size = " << code.size() << "\n";

    // Write each permutation, one per line
    for (const auto& p : code) {         // For each permutation in code
        for (int i = 0; i < n; i++) {    // For each element in permutation
            out << p[i] << (i + 1 == n ? "" : " ");  // Space-separated, no trailing space
        }
        out << "\n";                     // Newline after each permutation
    }
    out.flush();                         // Ensure data is written to disk
}

// Load a permutation array from file
static bool load_code(const string& filename, int n, vector<vector<int>>& code) {
    ifstream in(filename);               // Open file for reading
    if (!in) return false;               // Return false if file doesn't exist

    code.clear();                        // Clear output vector
    string line;                         // Buffer for each line

    // Read file line by line
    while (getline(in, line)) {
        if (line.empty()) continue;      // Skip empty lines
        if (line[0] == '#') continue;    // Skip comment lines

        stringstream ss(line);           // Parse line as space-separated integers
        vector<int> p(n);                // Create permutation vector

        for (int i = 0; i < n; i++) {    // Read n integers
            if (!(ss >> p[i]))           // If can't read integer
                return true;             // Stop if malformed (but return success for partial read)
        }
        code.push_back(std::move(p));    // Add permutation to code
    }
    return true;                         // Successfully loaded
}

// -------------------------
// Ulam distance calculation with early exit optimization
// Computes d(p,q) = n - LIS(invQ ∘ p) where invQ is the inverse of q
// Returns the distance, or 0 if distance is less than threshold (early exit)
// -------------------------
static inline int ulam_distance_at_least(int n, const int* p, const int* invQ, int threshold) {
    // Mathematical relationship:
    // d(p,q) >= threshold  <=>  n - LIS >= threshold  <=>  LIS <= n - threshold
    const int maxAllowedLIS = n - threshold;  // Maximum LIS allowed to meet threshold

    // Array to store LIS computation
    // tails[k] = minimum possible tail value for an increasing subsequence of length k+1
    int tails[256];                      // Stack-allocated for speed (256 is max n we expect)
    int len = 0;                         // Current length of LIS

    // Compute LIS using efficient O(n log n) algorithm
    for (int i = 0; i < n; i++) {
        int x = invQ[p[i]];              // Compose: invQ(p[i])
        // Binary search for position where x should go
        int* it = lower_bound(tails, tails + len, x);

        if (it == tails + len) {         // x is larger than all elements
            tails[len++] = x;            // Extend LIS
            if (len > maxAllowedLIS)     // Early exit: LIS too long, distance will be < threshold
                return 0;                // Signal failure (distance insufficient)
        } else {
            *it = x;                     // Replace element to keep smallest tail
        }
    }
    return n - len;                      // Return Ulam distance
}

// Compute the inverse of a permutation
// If p[i] = j, then inv[j] = i
static vector<int> inverse_of(const vector<int>& p) {
    int n = (int)p.size();               // Get permutation length
    vector<int> inv(n);                  // Allocate inverse array

    for (int i = 0; i < n; i++)          // For each position i
        inv[p[i]] = i;                   // If p[i]=j, then inv[j]=i

    return inv;                          // Return inverse permutation
}

// Validate that a vector is a valid permutation of {0, 1, ..., n-1}
static bool is_perm_0_to_n_minus_1(const vector<int>& p) {
    int n = (int)p.size();               // Get length
    vector<int> seen(n, 0);              // Track which values we've seen

    for (int x : p) {                    // For each element in p
        if (x < 0 || x >= n)             // Check if out of range
            return false;
        if (seen[x]++)                   // Check if already seen (duplicate)
            return false;
    }
    return true;                         // All checks passed - valid permutation
}

// Verify all pairwise distances in a code are at least d
static bool check_all_distances(const vector<vector<int>>& code, const vector<vector<int>>& invs, int n, int d) {
    // Check all pairs of permutations
    for (size_t i = 0; i < code.size(); i++) {
        for (size_t j = i + 1; j < code.size(); j++) {
            // Compute full distance (threshold=0 means compute exact distance)
            int dist = ulam_distance_at_least(n, code[i].data(), invs[j].data(), 0);

            if (dist < d) {              // Distance too small - code is invalid
                cerr << "[BAD] Pair i=" << i << " j=" << j
                     << " has distance " << dist << " < " << d << "\n";
                return false;
            }
        }
    }
    return true;                         // All distances are sufficient
}

// -------------------------
// Search / Improve - Core functions for improving permutation arrays
// -------------------------

// Check if permutation p can be added to the code
// Returns true if d(p, q) >= d for all q in code
static bool can_add(const vector<int>& p,
                    const vector<vector<int>>& code,
                    const vector<vector<int>>& invs,
                    int n, int d) {
    // Check distance to every permutation already in code
    for (size_t j = 0; j < code.size(); j++) {
        // If distance is less than d, cannot add
        if (ulam_distance_at_least(n, p.data(), invs[j].data(), d) < d)
            return false;                // Distance insufficient - cannot add
    }
    return true;                         // All distances sufficient - can add
}

// Update global best if current code is better (thread-safe)
static void update_best_if_needed(const vector<vector<int>>& code,
                                 const vector<vector<int>>& invs,
                                 int n, int d) {
    lock_guard<mutex> lk(best_mtx);      // Acquire lock for thread safety

    if (code.size() > best_code.size()) { // Found a better solution
        best_code = code;                // Save code
        best_inv = invs;                 // Save inverses
        save_code(best_filename(n, d), n, d, best_code);  // Write to file
        best_last_save = chrono::steady_clock::now();     // Record save time
        cout << "\n[NEW BEST] size = " << best_code.size()
             << " -> saved " << best_filename(n, d) << "\n";
    }
}

// Periodically save best code (to avoid losing progress)
static void periodic_best_save(int n, int d, int seconds = 60) {
    lock_guard<mutex> lk(best_mtx);      // Acquire lock

    auto now = chrono::steady_clock::now();  // Current time
    auto elapsed = chrono::duration_cast<chrono::seconds>(now - best_last_save).count();

    if (elapsed >= seconds) {            // Enough time has passed
        save_code(best_filename(n, d), n, d, best_code);  // Save best code
        best_last_save = now;            // Update last save time
        cout << "\n[SAVE BEST] size = " << best_code.size() << "\n";
    }
}

// Signal handler for Ctrl+C - sets STOP flag to gracefully terminate
static void handle_sigint(int) {
    STOP.store(true);                    // Set atomic flag (thread-safe)
}

// "Kick" operation: remove r random permutations to escape local optimum
// Keeps at least 1 permutation (first one acts as anchor)
static void kick(vector<vector<int>>& code, vector<vector<int>>& invs, mt19937& rng, int r) {
    if ((int)code.size() <= 1) return;   // Need at least 2 to remove any
    r = min(r, (int)code.size() - 1);    // Can't remove all - keep at least 1

    uniform_int_distribution<int> dist(0, (int)code.size() - 1);  // Random index generator

    // Choose r unique indices to remove
    vector<int> idx;                     // Indices to remove
    idx.reserve(r);
    while ((int)idx.size() < r) {
        int t = dist(rng);               // Pick random index
        if (t == 0) continue;            // Keep first permutation as anchor
        // Check if not already selected
        if (find(idx.begin(), idx.end(), t) == idx.end())
            idx.push_back(t);            // Add to removal list
    }

    // Sort in descending order (remove from back to avoid index shifts)
    sort(idx.begin(), idx.end(), greater<int>());

    // Remove selected permutations
    for (int t : idx) {
        code.erase(code.begin() + t);    // Remove from code
        invs.erase(invs.begin() + t);    // Remove corresponding inverse
    }
}

// Greedy fill: try to add random permutations to rebuild code after kick
// Attempts up to max_trials random permutations
static void greedy_fill(vector<vector<int>>& code,
                        vector<vector<int>>& invs,
                        int n, int d,
                        mt19937& rng,
                        long long max_trials) {
    vector<int> p(n);                    // Working permutation
    iota(p.begin(), p.end(), 0);         // Initialize to [0, 1, 2, ..., n-1]

    // Try adding random permutations
    for (long long t = 0; t < max_trials && !STOP.load(); t++) {
        shuffle(p.begin(), p.end(), rng); // Generate random permutation
        if (can_add(p, code, invs, n, d)) {  // Check if it can be added
            code.push_back(p);            // Add to code
            invs.push_back(inverse_of(p));  // Add its inverse
        }
    }
}

// -------------------------
// Main improvement loop - the core algorithm
// Strategy:
//   1. Try adding random permutations
//   2. If stuck (plateau), remove some permutations and try to rebuild
//   3. Save progress periodically
// -------------------------
static void improve_forever(vector<vector<int>> seed_code,
                            int n, int d,
                            uint64_t seed) {
    mt19937 rng((uint32_t)seed);         // Initialize random number generator

    // Initialize working code from seed
    vector<vector<int>> code = std::move(seed_code);  // Move seed into working code
    vector<vector<int>> invs;            // Inverses of permutations in code
    invs.reserve(code.size());
    for (auto& p : code)                 // Compute inverse for each permutation
        invs.push_back(inverse_of(p));

    update_best_if_needed(code, invs, n, d);  // Initialize best with seed

    vector<int> p(n);                    // Working permutation for random trials
    iota(p.begin(), p.end(), 0);         // Initialize to [0, 1, ..., n-1]

    long long attempts_since_add = 0;    // Track attempts without improvement
    size_t last_size = code.size();      // Track size for detecting changes

    auto last_current_save = chrono::steady_clock::now();  // Time of last save

    // Main loop: keep trying until user stops (Ctrl+C)
    while (!STOP.load()) {
        shuffle(p.begin(), p.end(), rng); // Generate random permutation

        // Try to add random permutation to code
        if (can_add(p, code, invs, n, d)) {  // Check if p has sufficient distance
            code.push_back(p);            // Add permutation to code
            invs.push_back(inverse_of(p));  // Add its inverse
            attempts_since_add = 0;       // Reset plateau counter

            // Print progress every 10 additions
            if (code.size() % 10 == 0) {
                cout << "size=" << code.size() << " " << flush;
            }

            update_best_if_needed(code, invs, n, d);  // Check if this is new best
        } else {
            attempts_since_add++;         // Failed to add - increment counter
        }

        // Periodically save current code (even if not best)
        auto now = chrono::steady_clock::now();
        if (chrono::duration_cast<chrono::seconds>(now - last_current_save).count() >= 120) {
            save_code(current_filename(n, d), n, d, code);  // Save current
            last_current_save = now;      // Update save time
            cout << "\n[SAVE CURRENT] size = " << code.size() << "\n";
        }

        periodic_best_save(n, d, 60);     // Save best every 60 seconds

        // Plateau detection and escape strategy
        // If stuck for many attempts, we're at a local optimum
        const long long PLATEAU_TRIALS = 250000;   // Threshold for plateau detection
        if (attempts_since_add >= PLATEAU_TRIALS) {
            // Determine how many permutations to remove (kick harder for larger codes)
            int r = 2;                    // Default: remove 2
            if (code.size() >= 50) r = 4; // Larger codes need bigger kicks
            if (code.size() >= 58) r = 6;

            cout << "\n[PLATEAU] stuck at size=" << code.size()
                 << " after " << attempts_since_add
                 << " failed adds. Kicking r=" << r << "...\n";

            kick(code, invs, rng, r);     // Remove r random permutations

            // Try to rebuild by greedily adding permutations
            greedy_fill(code, invs, n, d, rng, 400000);

            attempts_since_add = 0;       // Reset counter

            // If kick made us significantly worse, revert to best known solution
            if (code.size() + 2 < best_code.size()) {
                lock_guard<mutex> lk(best_mtx);  // Lock to read best
                cout << "[RESTART] reverting to best size=" << best_code.size() << "\n";
                code = best_code;         // Restore best code
                invs = best_inv;          // Restore inverses
            }

            // Print status after kick+refill
            cout << "[AFTER KICK] size=" << code.size() << "\n";
            update_best_if_needed(code, invs, n, d);  // Update best if improved
        }

        // Track size changes
        if (code.size() != last_size) {
            last_size = code.size();      // Update tracked size
        }
    }

    // Save final results when stopping
    {
        lock_guard<mutex> lk(best_mtx);   // Lock to access best
        save_code(best_filename(n, d), n, d, best_code);  // Save best
    }
    save_code(current_filename(n, d), n, d, code);  // Save current
    cout << "\n[STOP] Saved best + current.\n";
}

// -------------------------
// Main - Entry point for improve_pa program
// -------------------------
int main(int argc, char** argv) {
    // Parse command line arguments
    if (argc < 4) {
        cerr << "Usage: ./improve_pa n d seed_file.txt [seed]\n";
        return 1;
    }

    int n = stoi(argv[1]);               // Permutation length
    int d = stoi(argv[2]);               // Minimum Ulam distance required
    string seedfile = argv[3];           // File containing initial code

    // Random seed for RNG (use current time if not specified)
    uint64_t seed = (uint64_t)chrono::high_resolution_clock::now().time_since_epoch().count();
    if (argc >= 5)                       // If seed provided on command line
        seed = (uint64_t)stoull(argv[4]); // Use it instead

    // Set global parameters
    GLOBAL_N = n;
    GLOBAL_D = d;

    // Register Ctrl+C handler for graceful shutdown
    signal(SIGINT, handle_sigint);

    // Load seed code from file
    vector<vector<int>> seed_code;
    if (!load_code(seedfile, n, seed_code) || seed_code.empty()) {
        cerr << "Could not load seed file or it was empty: " << seedfile << "\n";
        return 1;
    }

    // Validate that each entry is a valid permutation
    for (size_t i = 0; i < seed_code.size(); i++) {
        if (!is_perm_0_to_n_minus_1(seed_code[i])) {
            cerr << "Seed file contains a non-permutation on line index " << i << "\n";
            return 1;
        }
    }

    // Build inverses for all seed permutations
    vector<vector<int>> invs;
    invs.reserve(seed_code.size());
    for (auto& p : seed_code)            // For each permutation
        invs.push_back(inverse_of(p));   // Compute and store its inverse

    // Verify seed is a valid permutation array (all distances >= d)
    cout << "[LOAD] seed size = " << seed_code.size() << "\n";
    cout << "[CHECK] verifying pairwise distances >= " << d << " ...\n";
    if (!check_all_distances(seed_code, invs, n, d)) {
        cerr << "Seed is NOT a valid U(" << n << "," << d << ") array. Fix seed first.\n";
        return 1;
    }
    cout << "[OK] seed is valid.\n";

    // Initialize best solution with seed
    {
        lock_guard<mutex> lk(best_mtx);   // Lock for thread safety
        best_code = seed_code;            // Set initial best
        best_inv = invs;                  // Set inverses
        best_last_save = chrono::steady_clock::now();  // Record save time
        save_code(best_filename(n, d), n, d, best_code);  // Save initial best
    }

    // Print program info
    cout << "===========================================\n";
    cout << "IMPROVE U(" << n << "," << d << ") FROM SEED\n";
    cout << "Seed file: " << seedfile << "\n";
    cout << "Initial size: " << seed_code.size() << "\n";
    cout << "Output best:  " << best_filename(n, d) << "\n";
    cout << "Output cur:   " << current_filename(n, d) << "\n";
    cout << "Stop with Ctrl+C (saves automatically)\n";
    cout << "===========================================\n";

    // Run main improvement algorithm (loops until Ctrl+C)
    improve_forever(seed_code, n, d, seed);

    return 0;
}
