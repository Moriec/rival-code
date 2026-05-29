import sys
from collections import defaultdict

class DSU:
    def __init__(self, size):
        self.parent = list(range(size + 1))  # 1-based
        self.rank = [0]*(size + 1)
    
    def find(self, x):
        if self.parent[x] != x:
            self.parent[x] = self.find(self.parent[x])
        return self.parent[x]
    
    def union(self, x, y):
        x_root = self.find(x)
        y_root = self.find(y)
        if x_root == y_root:
            return
        if self.rank[x_root] < self.rank[y_root]:
            self.parent[x_root] = y_root
        else:
            self.parent[y_root] = x_root
            if self.rank[x_root] == self.rank[y_root]:
                self.rank[x_root] += 1

def main(input_path, output_path, submission_path):
    with open(input_path) as f:
        n, q = map(int, f.readline().split())
        queries = []
        for _ in range(q):
            x, y = map(int, f.readline().split())
            queries.append((x, y))
    
    # Compute minimal_sum
    dsu = DSU(n)
    for x, y in queries:
        dsu.union(x, y)
    roots_count = defaultdict(int)
    for x, y in queries:
        root = dsu.find(x)
        roots_count[root] += 1
    minimal_sum = sum(cnt % 2 for cnt in roots_count.values())
    
    # Read submission output
    with open(submission_path) as f:
        submission_lines = [line.strip() for line in f]
    
    if len(submission_lines) != q:
        print(0)
        return
    
    a = [0] * (n + 1)  # 1-based
    valid = True
    for i in range(q):
        line = submission_lines[i]
        if len(line) != 2:
            valid = False
            break
        choice, sign = line[0], line[1]
        x, y = queries[i]
        if choice not in ('x', 'y'):
            valid = False
            break
        if sign not in ('+', '-'):
            valid = False
            break
        p = x if choice == 'x' else y
        d = 1 if sign == '+' else -1
        a[p] += d
        # Check all a are >=0
        for val in a[1:]:  # elements 1 to n
            if val < 0:
                valid = False
                break
        if not valid:
            break
    
    if not valid:
        print(0)
        return
    
    sum_a = sum(a[1:n+1])
    if sum_a == minimal_sum:
        print(1)
    else:
        print(0)

if __name__ == "__main__":
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
