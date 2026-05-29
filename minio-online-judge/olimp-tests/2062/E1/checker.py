import sys

def main(input_path, output_path, submission_output_path):
    with open(input_path) as f_in, open(submission_output_path) as f_sub:
        input_data = f_in.read().split()
        sub_output = f_sub.read().split()
    ptr = 0
    t = int(input_data[ptr])
    ptr += 1
    case_ptr = 0
    for _ in range(t):
        n = int(input_data[ptr])
        ptr += 1
        w = list(map(int, input_data[ptr:ptr+n]))
        ptr += n
        adj = [[] for _ in range(n+1)]
        for __ in range(n-1):
            u = int(input_data[ptr])
            v = int(input_data[ptr+1])
            adj[u].append(v)
            adj[v].append(u)
            ptr += 2
        # Compute dfn and low via DFS
        dfn = [0] * (n+1)
        low = [0] * (n+1)
        time = 1
        stack = [(1, None, False)]
        while stack:
            node, parent, visited = stack.pop()
            if visited:
                low[node] = time
                time += 1
                continue
            dfn[node] = time
            time += 1
            stack.append((node, parent, True))
            # Process children
            children = []
            for neighbor in adj[node]:
                if neighbor != parent:
                    children.append(neighbor)
            # To process in order, reverse the children
            for child in reversed(children):
                stack.append((child, node, False))
        # Precompute for each node x whether there exists v outside x's subtree with w_v > w[x]
        S = set()
        nodes_sorted = sorted(range(1, n+1), key=lambda x: dfn[x])
        prefix_max = [0]*(n+1)
        current_max = 0
        for i in range(n):
            current_max = max(current_max, w[nodes_sorted[i]-1])
            prefix_max[i+1] = current_max
        suffix_max = [0]*(n+2)
        current_max = 0
        for i in range(n-1, -1, -1):
            current_max = max(current_max, w[nodes_sorted[i]-1])
            suffix_max[i] = current_max
        suffix_max[n] = 0
        for x in range(1, n+1):
            # Binary search for the start and end of x's subtree in nodes_sorted
            left, right = 0, n-1
            start = n
            while left <= right:
                mid = (left + right) // 2
                if dfn[nodes_sorted[mid]] >= dfn[x]:
                    start = mid
                    right = mid - 1
                else:
                    left = mid + 1
            left, right = 0, n-1
            end = -1
            while left <= right:
                mid = (left + right) // 2
                if dfn[nodes_sorted[mid]] <= low[x]:
                    end = mid
                    left = mid + 1
                else:
                    right = mid - 1
            max_left = prefix_max[start]
            max_right = suffix_max[end+1] if end+1 < n else 0
            max_total = max(max_left, max_right)
            if max_total > w[x-1]:
                S.add(x)
        submitted = sub_output[case_ptr]
        case_ptr += 1
        if submitted == '0':
            if len(S) == 0:
                continue
            else:
                print(0)
                return
        else:
            u = int(submitted)
            if 1 <= u <= n and u in S:
                continue
            else:
                print(0)
                return
    print(1)

if __name__ == "__main__":
    import sys
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_output_path = sys.argv[3]
    main(input_path, output_path, submission_output_path)
