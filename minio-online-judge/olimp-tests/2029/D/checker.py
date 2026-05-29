import sys
from sys import argv

def main():
    input_path = argv[1]
    correct_output_path = argv[2]
    submission_output_path = argv[3]

    with open(input_path) as f:
        input_data = f.read().splitlines()
    ptr = 0
    t = int(input_data[ptr])
    ptr += 1

    with open(submission_output_path) as f:
        sub_lines = [line.strip() for line in f if line.strip()]
    sub_ptr = 0

    for _ in range(t):
        n, m = map(int, input_data[ptr].split())
        ptr += 1
        edges = set()
        for __ in range(m):
            u, v = map(int, input_data[ptr].split())
            if u > v:
                u, v = v, u
            edges.add(frozenset((u, v)))
            ptr += 1

        if sub_ptr >= len(sub_lines):
            print(0)
            return
        k_line = sub_lines[sub_ptr]
        try:
            k = int(k_line)
        except:
            print(0)
            return
        sub_ptr += 1
        if k < 0 or k > 2 * max(n, m):
            print(0)
            return

        operations = []
        for __ in range(k):
            if sub_ptr >= len(sub_lines):
                print(0)
                return
            line = sub_lines[sub_ptr]
            parts = line.split()
            if len(parts) != 3:
                print(0)
                return
            try:
                a, b, c = map(int, parts)
            except:
                print(0)
                return
            if len({a, b, c}) != 3:
                print(0)
                return
            operations.append((a, b, c))
            sub_ptr += 1

        current_edges = set(edges)
        for a, b, c in operations:
            pairs = [(a, b), (b, c), (c, a)]
            for u, v in pairs:
                if u > v:
                    u, v = v, u
                e = frozenset((u, v))
                if e in current_edges:
                    current_edges.remove(e)
                else:
                    current_edges.add(e)

        m_final = len(current_edges)
        if m_final == 0:
            continue
        elif m_final == n - 1:
            parent = list(range(n + 1))

            def find(u):
                while parent[u] != u:
                    parent[u] = parent[parent[u]]
                    u = parent[u]
                return u

            for e in current_edges:
                u, v = e
                u_root = find(u)
                v_root = find(v)
                if u_root != v_root:
                    parent[v_root] = u_root

            root = find(1)
            is_connected = all(find(node) == root for node in range(1, n + 1))
            if not is_connected:
                print(0)
                return
        else:
            print(0)
            return

    print(1)

if __name__ == "__main__":
    main()
