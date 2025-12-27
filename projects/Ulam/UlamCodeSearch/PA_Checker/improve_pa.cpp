#include <algorithm>
#include <atomic>
#include <chrono>
#include <csignal>
#include <fstream>
#include <iostream>
#include <mutex>
#include <numeric>
#include <random>
#include <sstream>
#include <string>
#include <vector>

using namespace std;

static int GLOBAL_N = 0;
static int GLOBAL_D = 0;

static atomic<bool> STOP(false);

static mutex best_mtx;
static vector<vector<int>> best_code;
static vector<vector<int>> best_inv;
static chrono::steady_clock::time_point best_last_save;

// -------------------------
// Helpers: I/O
// -------------------------
static string best_filename(int n, int d) {
    return "best_U" + to_string(n) + "_" + to_string(d) + ".txt";
}
static string current_filename(int n, int d) {
    return "current_U" + to_string(n) + "_" + to_string(d) + ".txt";
}

static void save_code(const string& filename, int n, int d, const vector<vector<int>>& code) {
    ofstream out(filename);
    if (!out) return;
    out << "# U(" << n << "," << d << ") size = " << code.size() << "\n";
    for (const auto& p : code) {
        for (int i = 0; i < n; i++) {
            out << p[i] << (i + 1 == n ? "" : " ");
        }
        out << "\n";
    }
    out.flush();
}

static bool load_code(const string& filename, int n, vector<vector<int>>& code) {
    ifstream in(filename);
    if (!in) return false;

    code.clear();
    string line;
    while (getline(in, line)) {
        if (line.empty()) continue;
        if (line[0] == '#') continue;
        stringstream ss(line);
        vector<int> p(n);
        for (int i = 0; i < n; i++) {
            if (!(ss >> p[i])) return true; // stop if malformed tail
        }
        code.push_back(std::move(p));
    }
    return true;
}

// -------------------------
// Ulam distance via LIS of inv(q) ∘ p
// returns (n - LIS)
// With early exit for threshold checking.
// -------------------------
static inline int ulam_distance_at_least(int n, const int* p, const int* invQ, int threshold) {
    // need d(p,q) >= threshold  <=>  n - LIS >= threshold  <=>  LIS <= n - threshold
    const int maxAllowedLIS = n - threshold;

    // small n (like 7) -> simple stack array is fine, but use vector for generality
    // tails[k] = minimum possible tail value for an increasing subsequence of length k+1
    int tails[256];
    int len = 0;

    for (int i = 0; i < n; i++) {
        int x = invQ[p[i]];
        int* it = lower_bound(tails, tails + len, x);
        if (it == tails + len) {
            tails[len++] = x;
            if (len > maxAllowedLIS) return 0; // fail early
        } else {
            *it = x;
        }
    }
    return n - len;
}

static vector<int> inverse_of(const vector<int>& p) {
    int n = (int)p.size();
    vector<int> inv(n);
    for (int i = 0; i < n; i++) inv[p[i]] = i;
    return inv;
}

static bool is_perm_0_to_n_minus_1(const vector<int>& p) {
    int n = (int)p.size();
    vector<int> seen(n, 0);
    for (int x : p) {
        if (x < 0 || x >= n) return false;
        if (seen[x]++) return false;
    }
    return true;
}

static bool check_all_distances(const vector<vector<int>>& code, const vector<vector<int>>& invs, int n, int d) {
    for (size_t i = 0; i < code.size(); i++) {
        for (size_t j = i + 1; j < code.size(); j++) {
            int dist = ulam_distance_at_least(n, code[i].data(), invs[j].data(), 0); // full distance
            if (dist < d) {
                cerr << "[BAD] Pair i=" << i << " j=" << j << " has distance " << dist << " < " << d << "\n";
                return false;
            }
        }
    }
    return true;
}

// -------------------------
// Search / Improve
// -------------------------
static bool can_add(const vector<int>& p,
                    const vector<vector<int>>& code,
                    const vector<vector<int>>& invs,
                    int n, int d) {
    for (size_t j = 0; j < code.size(); j++) {
        if (ulam_distance_at_least(n, p.data(), invs[j].data(), d) < d) return false;
    }
    return true;
}

static void update_best_if_needed(const vector<vector<int>>& code,
                                 const vector<vector<int>>& invs,
                                 int n, int d) {
    lock_guard<mutex> lk(best_mtx);
    if (code.size() > best_code.size()) {
        best_code = code;
        best_inv = invs;
        save_code(best_filename(n, d), n, d, best_code);
        best_last_save = chrono::steady_clock::now();
        cout << "\n[NEW BEST] size = " << best_code.size() << " -> saved " << best_filename(n, d) << "\n";
    }
}

static void periodic_best_save(int n, int d, int seconds = 60) {
    lock_guard<mutex> lk(best_mtx);
    auto now = chrono::steady_clock::now();
    auto elapsed = chrono::duration_cast<chrono::seconds>(now - best_last_save).count();
    if (elapsed >= seconds) {
        save_code(best_filename(n, d), n, d, best_code);
        best_last_save = now;
        cout << "\n[SAVE BEST] size = " << best_code.size() << "\n";
    }
}

static void handle_sigint(int) {
    STOP.store(true);
}

// Kick: remove r random codewords (keeping at least 1).
static void kick(vector<vector<int>>& code, vector<vector<int>>& invs, mt19937& rng, int r) {
    if ((int)code.size() <= 1) return;
    r = min(r, (int)code.size() - 1);

    uniform_int_distribution<int> dist(0, (int)code.size() - 1);

    // choose indices to remove
    vector<int> idx;
    idx.reserve(r);
    while ((int)idx.size() < r) {
        int t = dist(rng);
        if (t == 0) continue; // keep the first one as an anchor (arbitrary)
        if (find(idx.begin(), idx.end(), t) == idx.end()) idx.push_back(t);
    }
    sort(idx.begin(), idx.end(), greater<int>());

    for (int t : idx) {
        code.erase(code.begin() + t);
        invs.erase(invs.begin() + t);
    }
}

// Greedy rebuild attempt: try to add as many as possible for some iterations.
static void greedy_fill(vector<vector<int>>& code,
                        vector<vector<int>>& invs,
                        int n, int d,
                        mt19937& rng,
                        long long max_trials) {
    vector<int> p(n);
    iota(p.begin(), p.end(), 0);

    for (long long t = 0; t < max_trials && !STOP.load(); t++) {
        shuffle(p.begin(), p.end(), rng);
        if (can_add(p, code, invs, n, d)) {
            code.push_back(p);
            invs.push_back(inverse_of(p));
        }
    }
}

// Main improvement loop:
// - try random additions
// - if stuck, kick + refill
static void improve_forever(vector<vector<int>> seed_code,
                            int n, int d,
                            uint64_t seed) {
    mt19937 rng((uint32_t)seed);

    vector<vector<int>> code = std::move(seed_code);
    vector<vector<int>> invs;
    invs.reserve(code.size());
    for (auto& p : code) invs.push_back(inverse_of(p));

    update_best_if_needed(code, invs, n, d);

    vector<int> p(n);
    iota(p.begin(), p.end(), 0);

    long long attempts_since_add = 0;
    size_t last_size = code.size();

    auto last_current_save = chrono::steady_clock::now();

    while (!STOP.load()) {
        shuffle(p.begin(), p.end(), rng);

        if (can_add(p, code, invs, n, d)) {
            code.push_back(p);
            invs.push_back(inverse_of(p));
            attempts_since_add = 0;

            if (code.size() % 10 == 0) {
                cout << "size=" << code.size() << " " << flush;
            }

            update_best_if_needed(code, invs, n, d);
        } else {
            attempts_since_add++;
        }

        // Save current occasionally (so you can see where it is even if it’s not best)
        auto now = chrono::steady_clock::now();
        if (chrono::duration_cast<chrono::seconds>(now - last_current_save).count() >= 120) {
            save_code(current_filename(n, d), n, d, code);
            last_current_save = now;
            cout << "\n[SAVE CURRENT] size = " << code.size() << "\n";
        }

        periodic_best_save(n, d, 60);

        // Plateau logic:
        // If we haven't added anything in a while, do a kick and try to refill.
        // Tune these numbers as needed.
        const long long PLATEAU_TRIALS = 250000;   // for n=7 this is quick
        if (attempts_since_add >= PLATEAU_TRIALS) {
            // If we’re not growing, kick harder.
            int r = 2;
            if (code.size() >= 50) r = 4;
            if (code.size() >= 58) r = 6;

            cout << "\n[PLATEAU] stuck at size=" << code.size()
                 << " after " << attempts_since_add
                 << " failed adds. Kicking r=" << r << "...\n";

            kick(code, invs, rng, r);

            // Refill
            greedy_fill(code, invs, n, d, rng, 400000);

            attempts_since_add = 0;

            // If kick made us worse and we have a better best, restart from best sometimes.
            if (code.size() + 2 < best_code.size()) {
                lock_guard<mutex> lk(best_mtx);
                cout << "[RESTART] reverting to best size=" << best_code.size() << "\n";
                code = best_code;
                invs = best_inv;
            }

            // print progress
            cout << "[AFTER KICK] size=" << code.size() << "\n";
            update_best_if_needed(code, invs, n, d);
        }

        // If we somehow shrink or stagnate weirdly, refresh tracking
        if (code.size() != last_size) {
            last_size = code.size();
        }
    }

    // final save on stop
    {
        lock_guard<mutex> lk(best_mtx);
        save_code(best_filename(n, d), n, d, best_code);
    }
    save_code(current_filename(n, d), n, d, code);
    cout << "\n[STOP] Saved best + current.\n";
}

int main(int argc, char** argv) {
    if (argc < 4) {
        cerr << "Usage: ./improve_pa n d seed_file.txt [seed]\n";
        return 1;
    }
    int n = stoi(argv[1]);
    int d = stoi(argv[2]);
    string seedfile = argv[3];
    uint64_t seed = (uint64_t)chrono::high_resolution_clock::now().time_since_epoch().count();
    if (argc >= 5) seed = (uint64_t)stoull(argv[4]);

    GLOBAL_N = n;
    GLOBAL_D = d;

    signal(SIGINT, handle_sigint);

    vector<vector<int>> seed_code;
    if (!load_code(seedfile, n, seed_code) || seed_code.empty()) {
        cerr << "Could not load seed file or it was empty: " << seedfile << "\n";
        return 1;
    }

    // Validate permutations
    for (size_t i = 0; i < seed_code.size(); i++) {
        if (!is_perm_0_to_n_minus_1(seed_code[i])) {
            cerr << "Seed file contains a non-permutation on line index " << i << "\n";
            return 1;
        }
    }

    // Build inverses + verify distances once
    vector<vector<int>> invs;
    invs.reserve(seed_code.size());
    for (auto& p : seed_code) invs.push_back(inverse_of(p));

    cout << "[LOAD] seed size = " << seed_code.size() << "\n";
    cout << "[CHECK] verifying pairwise distances >= " << d << " ...\n";
    if (!check_all_distances(seed_code, invs, n, d)) {
        cerr << "Seed is NOT a valid U(" << n << "," << d << ") array. Fix seed first.\n";
        return 1;
    }
    cout << "[OK] seed is valid.\n";

    // Initialize best from seed
    {
        lock_guard<mutex> lk(best_mtx);
        best_code = seed_code;
        best_inv = invs;
        best_last_save = chrono::steady_clock::now();
        save_code(best_filename(n, d), n, d, best_code);
    }

    cout << "===========================================\n";
    cout << "IMPROVE U(" << n << "," << d << ") FROM SEED\n";
    cout << "Seed file: " << seedfile << "\n";
    cout << "Initial size: " << seed_code.size() << "\n";
    cout << "Output best:  " << best_filename(n, d) << "\n";
    cout << "Output cur:   " << current_filename(n, d) << "\n";
    cout << "Stop with Ctrl+C (saves automatically)\n";
    cout << "===========================================\n";

    improve_forever(seed_code, n, d, seed);
    return 0;
}
