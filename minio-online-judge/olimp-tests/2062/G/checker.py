import sys

def readints(f):
    return list(map(int, f.readline().split()))

def process_case(input_case, ref_output, sub_output):
    n = input_case['n']
    p = input_case['p']
    q = input_case['q']
    original_p = p.copy()

    # Process reference output
    try:
        ref_k_line = ref_output.readline().strip()
        if not ref_k_line:
            return False
        ref_k = int(ref_k_line)
        if ref_k < 0 or ref_k > n * n:
            return False
        ref_swaps = []
        for _ in range(ref_k):
            parts = ref_output.readline().strip().split()
            if len(parts) != 2:
                return False
            i, j = map(int, parts)
            ref_swaps.append((i, j))
        ref_p = original_p.copy()
        ref_cost = 0
        for i, j in ref_swaps:
            if i < 1 or i > n or j < 1 or j > n or i == j:
                return False
            idx_i = i - 1
            idx_j = j - 1
            cost = min(abs(i - j), abs(ref_p[idx_i] - ref_p[idx_j]))
            ref_cost += cost
            ref_p[idx_i], ref_p[idx_j] = ref_p[idx_j], ref_p[idx_i]
        if ref_p != q:
            return False
    except Exception as e:
        return False

    # Process submission output
    try:
        sub_k_line = sub_output.readline().strip()
        if not sub_k_line:
            sub_k = 0
        else:
            sub_k = int(sub_k_line)
        if sub_k < 0 or sub_k > n * n:
            return False
        sub_swaps = []
        for _ in range(sub_k):
            parts = sub_output.readline().strip().split()
            if len(parts) != 2:
                return False
            i, j = map(int, parts)
            sub_swaps.append((i, j))
        sub_p = original_p.copy()
        sub_cost = 0
        for i, j in sub_swaps:
            if i < 1 or i > n or j < 1 or j > n or i == j:
                return False
            idx_i = i - 1
            idx_j = j - 1
            cost = min(abs(i - j), abs(sub_p[idx_i] - sub_p[idx_j]))
            sub_cost += cost
            sub_p[idx_i], sub_p[idx_j] = sub_p[idx_j], sub_p[idx_i]
        if sub_p != q or sub_cost != ref_cost:
            return False
    except Exception as e:
        return False
    return True

def main(input_path, ref_path, sub_path):
    with open(input_path) as input_file, open(ref_path) as ref_file, open(sub_path) as sub_file:
        t = int(input_file.readline())
        for _ in range(t):
            n = int(input_file.readline())
            p = list(map(int, input_file.readline().split()))
            q = list(map(int, input_file.readline().split()))
            input_case = {'n': n, 'p': p, 'q': q}
            if not process_case(input_case, ref_file, sub_file):
                print(0)
                return
        print(1)

if __name__ == '__main__':
    import sys
    input_path, ref_path, sub_path = sys.argv[1:4]
    main(input_path, ref_path, sub_path)
