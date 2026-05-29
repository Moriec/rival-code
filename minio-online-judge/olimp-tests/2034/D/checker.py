import sys

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path, 'r') as f:
        t = int(f.readline().strip())
        test_cases = []
        for _ in range(t):
            n = int(f.readline().strip())
            a = list(map(int, f.readline().strip().split()))
            test_cases.append((n, a))

    with open(submission_path, 'r') as f:
        lines = [line.strip() for line in f.readlines() if line.strip()]

    ptr = 0
    for case_idx in range(t):
        n, a = test_cases[case_idx]
        current_a = a.copy()

        if ptr >= len(lines):
            print(0)
            return
        try:
            k = int(lines[ptr])
        except:
            print(0)
            return
        ptr +=1

        if k <0 or k >n:
            print(0)
            return

        moves = []
        for _ in range(k):
            if ptr >= len(lines):
                print(0)
                return
            parts = lines[ptr].split()
            ptr +=1
            if len(parts) !=2:
                print(0)
                return
            try:
                u = int(parts[0])
                v = int(parts[1])
            except:
                print(0)
                return
            if not (1 <= u <=n and 1 <= v <=n):
                print(0)
                return
            moves.append((u, v))

        # Apply moves
        for u, v in moves:
            u_idx = u-1
            v_idx = v-1
            if abs(current_a[u_idx] - current_a[v_idx]) !=1:
                print(0)
                return
            if current_a[u_idx] > current_a[v_idx]:
                current_a[u_idx] -=1
                current_a[v_idx] +=1
            else:
                current_a[v_idx] -=1
                current_a[u_idx] +=1

        # Check sorted
        for i in range(n-1):
            if current_a[i] > current_a[i+1]:
                print(0)
                return

    print(1)

if __name__ == "__main__":
    main()
