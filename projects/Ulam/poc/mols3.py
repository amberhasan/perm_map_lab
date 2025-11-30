X = '''
1234
2143
3412
4321
'''

Y = '''
1234
3412
4321
2143
'''

def parse(X):
    A = dict()
    for r,row in enumerate(X.strip().split('\n')):
        for c,v in enumerate(row):
            A[(int(v),c+1)] = r+1
    return A


def pump(A):
    ret = []
    for r in range(1,5):
        row = []
        for c in range(1,5):
            row.append(A[(r,c)])
        ret.append(row)

    return ret

def pprint(B):
    for row in B:
        print(' '.join(map(str,row)))


for k,v in parse(Y).items():
    print(k,v)

A = []
A.extend(pump(parse(X)))
A.extend(pump(parse(Y)))

ret = 4
for ux,u in enumerate(A):
    print(''.join(map(str,u)))
    for vx in range(ux):
        v = A[vx]
        d = 0
        for x,y in zip(u,v):
            if x != y:
                d += 1
        if d < ret:
            ret = d

print(ret)