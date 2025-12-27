#include <iostream>
#include <vector>
#include <algorithm>
#include <numeric>
#include <string>
using namespace std;

/*
  Compute Ulam distance using LIS of pi^{-1} o sigma
*/
int ulam_distance(const vector<int>& p, const vector<int>& q) {
    int n = p.size();
    vector<int> inv_p(n);
    for (int i = 0; i < n; i++) inv_p[p[i]] = i;

    vector<int> seq(n);
    for (int i = 0; i < n; i++)
        seq[i] = inv_p[q[i]];

    vector<int> lis;
    for (int x : seq) {
        auto it = lower_bound(lis.begin(), lis.end(), x);
        if (it == lis.end()) lis.push_back(x);
        else *it = x;
    }
    return n - (int)lis.size();
}

/*
  Generate all permutations formed by inserting a and b
  into base permutation in all possible positions
*/
vector<vector<int>> insert_two_symbols(
    const vector<int>& base, int a, int b
) {
    int m = base.size();
    vector<vector<int>> result;

    for (int i = 0; i <= m; i++) {
        for (int j = 0; j <= m + 1; j++) {
            if (j == i) continue;

            vector<int> tmp;
            for (int k = 0; k < m + 2; k++) {
                if (k == i) tmp.push_back(a);
                else if (k == j) tmp.push_back(b);
                else {
                    int idx = k;
                    if (k > i) idx--;
                    if (k > j) idx--;
                    tmp.push_back(base[idx]);
                }
            }
            result.push_back(tmp);
        }
    }
    return result;
}

int main(int argc, char* argv[]) {
    if (argc < 2) {
        cout << "Usage: ./equiv_d3 n\n";
        return 1;
    }

    int n = stoi(argv[1]);
    int d = 3;

    int a = n - 2;
    int b = n - 1;

    vector<vector<int>> code;

    // Enumerate all permutations of {0, ..., n-3}
    vector<int> base(n - 2);
    iota(base.begin(), base.end(), 0);

    do {
        // Generate equivalence class
        auto eq_class = insert_two_symbols(base, a, b);

        // Try to select one representative
        for (const auto& p : eq_class) {
            bool ok = true;
            for (const auto& q : code) {
                if (ulam_distance(p, q) < d) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                code.push_back(p);
                break; // at most one per class
            }
        }

    } while (next_permutation(base.begin(), base.end()));

    cout << "Constructed code of size " << code.size()
         << " for U(" << n << ",3)\n";

    // Optional: print permutations
    for (const auto& p : code) {
        for (int x : p) cout << x << " ";
        cout << "\n";
    }

    return 0;
}
