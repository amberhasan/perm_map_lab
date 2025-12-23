#include <bits/stdc++.h>
using namespace std;

/**
 * Multistage "block shuffle" construction inspired by:
 * "Explicit Good Codes Approaching Distance 1 in Ulam Metric"
 *
 * Experimental PA builder for U(n,d) using structured shuffle-lift sampling.
 *
 * Example runs:
 *   ./UlamShuffleLift q=3 n=243 d=180 samples=20000
 *   ./UlamShuffleLift n=81 d=60
 */

// =====================================================
// Utilities
// =====================================================

int ipow(int a, int e) {
    int r = 1;
    for (int i = 0; i < e; i++) r *= a;
    return r;
}

int computeEllIfPower(int n, int q) {
    int ell = 0;
    int cur = 1;
    while (cur < n) {
        cur *= q;
        ell++;
    }
    if (cur != n) {
        throw runtime_error("n must be a power of q. Got n=" +
                            to_string(n) + ", q=" + to_string(q));
    }
    return ell;
}

// =====================================================
// Multistage Construction
// =====================================================

int blockIndexExcludingDigit(const vector<int>& dig, int q, int excludedDigit) {
    int idx = 0;
    for (int i = 0; i < (int)dig.size(); i++) {
        if (i != excludedDigit)
            idx = idx * q + dig[i];
    }
    return idx;
}

int replaceDigitAndPack(const vector<int>& dig, int q, int posDigit, int newVal) {
    int idx = 0;
    for (int i = 0; i < (int)dig.size(); i++) {
        int v = (i == posDigit ? newVal : dig[i]);
        idx = idx * q + v;
    }
    return idx;
}

vector<int> buildPermutation(
    int q,
    int ell,
    const vector<vector<int>>& D,
    const vector<vector<int>>& shufflers
) {
    int n = ipow(q, ell);

    if ((int)shufflers.size() != ell)
        throw runtime_error("Need exactly ell shufflers.");

    vector<int> pi(n);
    iota(pi.begin(), pi.end(), 0);

    // Precompute base-q digits
    vector<vector<int>> digits(n, vector<int>(ell));
    for (int pos = 0; pos < n; pos++) {
        int x = pos;
        for (int d = ell - 1; d >= 0; d--) {
            digits[pos][d] = x % q;
            x /= q;
        }
    }

    for (int stage = 0; stage < ell; stage++) {
        int blocks = n / q;
        if ((int)shufflers[stage].size() != blocks)
            throw runtime_error("Invalid shuffler length at stage " +
                                to_string(stage));

        vector<int> next(n);

        for (int pos = 0; pos < n; pos++) {
            int blockIndex = blockIndexExcludingDigit(digits[pos], q, stage);
            int c = shufflers[stage][blockIndex];
            int x = digits[pos][stage];
            int y = D[c][x];

            int srcPos = replaceDigitAndPack(digits[pos], q, stage, y);
            next[pos] = pi[srcPos];
        }
        pi.swap(next);
    }
    return pi;
}

// =====================================================
// Ulam Distance
// =====================================================

int lisLength(const vector<int>& a) {
    vector<int> tails;
    for (int x : a) {
        auto it = lower_bound(tails.begin(), tails.end(), x);
        if (it == tails.end())
            tails.push_back(x);
        else
            *it = x;
    }
    return (int)tails.size();
}

int ulamDistance(const vector<int>& pi, const vector<int>& pj) {
    int n = pi.size();
    vector<int> inv(n);
    for (int i = 0; i < n; i++) inv[pi[i]] = i;

    vector<int> seq(n);
    for (int i = 0; i < n; i++) seq[i] = inv[pj[i]];

    return n - lisLength(seq);
}

// =====================================================
// Build PA via sampling
// =====================================================

vector<vector<int>> buildUlamPA(
    int q,
    int ell,
    const vector<vector<int>>& D,
    int samples,
    int minDistance
) {
    mt19937 rng(random_device{}());
    int n = ipow(q, ell);
    int blocks = n / q;
    int p = D.size();

    vector<vector<int>> PA;

    for (int t = 0; t < samples; t++) {
        vector<vector<int>> shufflers(ell, vector<int>(blocks));
        for (int i = 0; i < ell; i++)
            for (int j = 0; j < blocks; j++)
                shufflers[i][j] = rng() % p;

        vector<int> pi = buildPermutation(q, ell, D, shufflers);

        bool ok = true;
        for (const auto& pj : PA) {
            if (ulamDistance(pi, pj) < minDistance) {
                ok = false;
                break;
            }
        }
        if (ok) PA.push_back(move(pi));
    }
    return PA;
}

// =====================================================
// Argument Parsing
// =====================================================

unordered_map<string, int> parseArgs(int argc, char* argv[]) {
    unordered_map<string, int> params;
    for (int i = 1; i < argc; i++) {
        string s(argv[i]);
        auto pos = s.find('=');
        if (pos == string::npos) continue;
        string key = s.substr(0, pos);
        int val = stoi(s.substr(pos + 1));
        params[key] = val;
    }
    return params;
}

// =====================================================
// Main
// =====================================================

int main(int argc, char* argv[]) {

    // Defaults
    int q = 3;
    int n = 81;
    int d = 60;
    int samples = 50000;

    auto params = parseArgs(argc, argv);
    if (params.count("q")) q = params["q"];
    if (params.count("n")) n = params["n"];
    if (params.count("d")) d = params["d"];
    if (params.count("samples")) samples = params["samples"];

    int ell;
    try {
        ell = computeEllIfPower(n, q);
    } catch (const exception& e) {
        cerr << e.what() << endl;
        return 1;
    }

    // Ground permutations D ⊆ S_q
    vector<vector<int>> D = {
        {0,1,2},
        {2,1,0},
        {1,0,2},
        {1,2,0}
    };

    cout << "====================================\n";
    cout << "UlamShuffleLift experiment\n";
    cout << "q = " << q << "\n";
    cout << "n = " << n << "\n";
    cout << "ell = " << ell << "\n";
    cout << "d = " << d << "\n";
    cout << "samples = " << samples << "\n";
    cout << "====================================\n";

    auto PA = buildUlamPA(q, ell, D, samples, d);

    cout << "PA size = " << PA.size() << "\n";

    // Write results
    string filename = "ulam_results_q" + to_string(q) +
                      "_n" + to_string(n) +
                      "_d" + to_string(d) + ".txt";

    ofstream out(filename, ios::app);
    out << "====================================\n";
    out << "Timestamp: " << time(nullptr) << "\n";
    out << "q = " << q << "\n";
    out << "n = " << n << "\n";
    out << "ell = " << ell << "\n";
    out << "d = " << d << "\n";
    out << "samples = " << samples << "\n";
    out << "PA size = " << PA.size() << "\n\n";
    out.close();

    cout << "Results written to " << filename << "\n";
    return 0;
}
