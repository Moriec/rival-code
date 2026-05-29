import sys
from collections import deque, defaultdict

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    # Read input data
    with open(input_path, 'r') as f:
        n = int(f.readline().strip())
        a = list(map(int, f.readline().strip().split()))
        edges = [tuple(map(int, line.strip().split())) for line in f]

    # Read reference output
    with open(output_path, 'r') as f:
        ref_lines = f.readlines()
        if len(ref_lines) < 2:
            print(0)
            return
        ref_k = int(ref_lines[0].strip())
        ref_vertices = list(map(int, ref_lines[1].strip().split()))
        if len(ref_vertices) != ref_k or len(set(ref_vertices)) != ref_k:
            print(0)
            return

    # Read submission output
    with open(submission_path, 'r') as f:
        sub_lines = f.readlines()
        if len(sub_lines) < 2:
            print(0)
            return
        try:
            sub_k = int(sub_lines[0].strip())
            sub_vertices = list(map(int, sub_lines[1].strip().split()))
            if len(sub_vertices) != sub_k or len(set(sub_vertices)) != sub_k:
                print(0)
                return
        except:
            print(0)
            return

    # Check sum equality
    ref_sum = sum(1 << (v-1) for v in ref_vertices)  # 2^(v-1) because 2^v in problem?
    sub_sum = sum(1 << (v-1) for v in sub_vertices)
    if ref_sum != sub_sum:
        print(0)
        return

    # Check coverage of all values
    present = [False] * (n + 1)
    for v in sub_vertices:
        val = a[v-1]  # a is 0-based in list but vertices are 1-based
        present[val] = True
    if not all(present[val] for val in range(1, n+1)):
        print(0)
        return

    # Build adjacency list
    adj = defaultdict(list)
    for u, v in edges:
        adj[u].append(v)
        adj[v].append(u)

    # Check connectedness using BFS
    sub_set = set(sub_vertices)
    if not sub_set:
        print(0)
        return
    visited = set()
    q = deque()
    q.append(sub_vertices[0])
    visited.add(sub_vertices[0])
    while q:
        u = q.popleft()
        for v in adj[u]:
            if v in sub_set and v not in visited:
                visited.add(v)
                q.append(v)
    if visited != sub_set:
        print(0)
        return

    print(1)

if __name__ == "__main__":
    main()
