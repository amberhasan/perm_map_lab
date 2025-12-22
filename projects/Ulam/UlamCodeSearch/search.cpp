#include <iostream>
#include <vector>
#include <numeric>
#include <algorithm>
#include <random>
#include <ctime>
#include <fstream>
#include <chrono>
#include <string>
#include <dispatch/dispatch.h>
#include <signal.h>

using namespace std;

// Global variables (Accessible by threads and the Ctrl+C handler)
vector<vector<int> > code;
vector<vector<int> > inverses;
int global_n, global_d;
chrono::steady_clock::time_point lastSave;

// High-speed LIS distance check
int ulamDistanceAtLeast(int n, const int* p, const int* invQ, int threshold) {
    int maxAllowed = n - threshold;
    int tails[16]; 
    int size = 0;
    for (int i = 0; i < n; i++) {
        int x = invQ[p[i]];
        int* it = lower_bound(tails, tails + size, x);
        if (it == tails + size) tails[size++] = x;
        else *it = x;
        if (size > maxAllowed) return 0;
    }
    return n - size;
}

// Function to save the current code to a text file
void saveToFile(int n, int d, const vector<vector<int> >& currentCode) {
    string filename = "U" + to_string(n) + "_" + to_string(d) + "_results.txt";
    ofstream out(filename.c_str());
    if (!out) return;

    out << "# U(" << n << "," << d << ") size = " << currentCode.size() << endl;
    for (size_t i = 0; i < currentCode.size(); ++i) {
        for (int j = 0; j < n; j++) {
            out << currentCode[i][j] << (j == n - 1 ? "" : " ");
        }
        out << endl;
    }
    out.flush();
    cout << "\n[SAVE] File updated: " << currentCode.size() << " codewords found." << endl;
}

// Handler to save progress when you press Ctrl+C
void handle_sigint(int sig) {
    cout << "\n[STOP] Signal received. Performing final save..." << endl;
    saveToFile(global_n, global_d, code);
    exit(sig);
}

int main(int argc, char* argv[]) {
    if (argc < 3) {
        cout << "Usage: ./search 12 4" << endl;
        return 1;
    }
    global_n = stoi(argv[1]);
    global_d = stoi(argv[2]);

    // Register the Ctrl+C handler
    signal(SIGINT, handle_sigint);

    // Sync queue to prevent threads from writing to the list at the same time
    dispatch_queue_t syncQueue = dispatch_queue_create("com.ulam.sync", DISPATCH_QUEUE_SERIAL);

    // Initial identity permutation [0, 1, 2, ... n-1]
    vector<int> first(global_n);
    iota(first.begin(), first.end(), 0);
    code.push_back(first);
    vector<int> firstInv(global_n);
    for(int i=0; i<global_n; i++) firstInv[first[i]] = i;
    inverses.push_back(firstInv);

    lastSave = chrono::steady_clock::now();

    cout << "===========================================" << endl;
    cout << "ULAM CODE SEARCH: U(" << global_n << ", " << global_d << ")" << endl;
    cout << "Running on Apple Silicon Threads..." << endl;
    cout << "Saving every 100 codewords or 5 minutes." << endl;
    cout << "===========================================" << endl;

    // Launch threads
    dispatch_apply(64, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^(size_t iteration) {
        mt19937 g(static_cast<uint32_t>(time(0) ^ (iteration * 1337)));
        vector<int> p(global_n);
        iota(p.begin(), p.end(), 0);

        while (true) {
            shuffle(p.begin(), p.end(), g);
            __block bool ok = true;

            dispatch_sync(syncQueue, ^{
                // Check against existing codewords
                for (size_t j = 0; j < code.size(); ++j) {
                    if (ulamDistanceAtLeast(global_n, p.data(), inverses[j].data(), global_d) < global_d) {
                        ok = false;
                        break;
                    }
                }
                
                if (ok) {
                    code.push_back(p);
                    vector<int> inv(global_n);
                    for(int k=0; k<global_n; k++) inv[p[k]] = k;
                    inverses.push_back(inv);
                    
                    if (code.size() % 50 == 0) {
                        cout << "Count: " << code.size() << " " << flush;
                    }

                    auto now = chrono::steady_clock::now();
                    auto elapsed = chrono::duration_cast<chrono::minutes>(now - lastSave).count();
                    
                    if (code.size() % 100 == 0 || elapsed >= 5) {
                        saveToFile(global_n, global_d, code);
                        lastSave = now;
                    }
                }
            });
        }
    });

    return 0;
}