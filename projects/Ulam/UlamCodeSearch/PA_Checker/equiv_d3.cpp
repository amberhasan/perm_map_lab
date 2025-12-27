#include <iostream>
#include <vector>
#include <algorithm>
#include <numeric>
#include <random>
#include <chrono>
#include <csignal>
#include <fstream>
#include <string>

using namespace std;

// ---------------- CONFIG ----------------
static const int D = 3;
static const int MAX_PER_CLASS = 1;   // increase to 3 if needed
// ----------------------------------------

// Global tracking
int BEST = 0;
vector<vector<int>> BEST_CODE;
int GLOBAL_N;

// ----------------------------------------
// Save best code to file
// ----------------------------------------
void save_best() {
    string filename = "U" + to_string(GLOBAL_N) + "_d3_equiv.txt";
    ofstream out(filename);
    if (!out) return;

    out << "# Best known U(" << GLOBAL_N << ",3) >= "
        << BEST << "\n";

    for (const auto& p : BEST_CODE) {
        for (int x : p)
            out << x << " ";
        out << "\n";
    }

    out.close();
    cout << "[SAVE] Wrote " << BEST
         << " permutations to " << filename << endl;
}

// ----------------------------------------
// Ctrl+C handler
// ----------------------------------------
void handle_sigint(int) {
    cout << "\n[STOP] Final best = " << BEST << endl;
    save_best();
    exit(0);
}

// ----------------------------------------
// Ulam distance via LIS(pi^{-1} ∘ sigma)
// ----------------------------------------
int ulam_distance(const vector<int>& p, const vector<int>& q) {
    int n = p.size();
    vector<int> inv(n);
    for (int i = 0; i < n; i++)
        inv[p[i]] = i;

    vector<int> seq(n);
    for (int i = 0; i < n; i++)
        seq[i] = inv[q[i]];

    vector<int> lis;
    for (int x : seq) {
        auto it = lower_bound(lis.begin(), lis.end(), x);
        if (it == lis.end()) lis.push_back(x);
        else *it = x;
    }
    return n - (int)lis.size();
}

// ----------------------------------------
// Insert two symbols into all positions
// ----------------------------------------
vector<vector<int>> insert_two(const vector<int>& base, int a, int b) {
    int m = base.size();
    vector<vector<int>> res;

    for (int i = 0; i <= m; i++) {
        for (int j = 0; j <= m + 1; j++) {
            if (j == i) continue;

            vector<int> p;
            for (int k = 0; k < m + 2; k++) {
                if (k == i) p.push_back(a);
                else if (k == j) p.push_back(b);
                else {
                    int idx = k;
                    if (k > i) idx--;
                    if (k > j) idx--;
                    p.push_back(base[idx]);
                }
            }
            res.push_back(p);
        }
    }
    return res;
}

// ----------------------------------------
// Main
// ----------------------------------------
int main(int argc, char* argv[]) {
    if (argc < 2) {
        cout << "Usage: ./equiv_d3 n\n";
        return 1;
    }

    GLOBAL_N = stoi(argv[1]);
    int n = GLOBAL_N;
    int a = n - 2;
    int b = n - 1;

    signal(SIGINT, handle_sigint);

    mt19937 rng(chrono::high_resolution_clock::now()
                    .time_since_epoch().count());

    long long ITER = 0;

    while (true) {
        ITER++;

        vector<vector<int>> code;

        vector<int> base(n - 2);
        iota(base.begin(), base.end(), 0);

        do {
            auto eq = insert_two(base, a, b);
            shuffle(eq.begin(), eq.end(), rng);

            int taken = 0;
            for (const auto& p : eq) {
                bool ok = true;
                for (const auto& q : code) {
                    if (ulam_distance(p, q) < D) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    code.push_back(p);
                    taken++;
                    if (taken >= MAX_PER_CLASS)
                        break;
                }
            }

        } while (next_permutation(base.begin(), base.end()));

        int sz = (int)code.size();

        if (sz > BEST) {
            BEST = sz;
            BEST_CODE = code;
            cout << "[NEW BEST] U(" << n << ",3) ≥ "
                 << BEST << "   (iter " << ITER << ")" << endl;
            save_best();
        }

        if (ITER % 50 == 0) {
            cout << "[iter " << ITER
                 << "] current=" << sz
                 << "  best=" << BEST << endl;
        }
    }

    return 0;
}
