import sys

def main():
    input_path = sys.argv[1]
    submission_path = sys.argv[3]

    with open(input_path) as f_in:
        t = int(f_in.readline())
        test_cases = []
        for _ in range(t):
            n = int(f_in.readline().strip())
            p = list(map(int, f_in.readline().split()))
            test_cases.append((n, p))

    with open(submission_path) as f_sub:
        for (n, p) in test_cases:
            line = f_sub.readline().strip()
            if not line:
                print(0)
                return
            q = list(map(int, line.split()))

            # Check if q is a permutation of p
            if sorted(p) != sorted(q):
                print(0)
                return

            # Compute prefix sums
            sum_p = [0] * (n + 1)
            sum_q = [0] * (n + 1)
            for i in range(1, n + 1):
                sum_p[i] = sum_p[i-1] + p[i-1]
                sum_q[i] = sum_q[i-1] + q[i-1]

            # Check differences
            seen = set()
            for k in range(n + 1):
                diff = sum_p[k] - sum_q[k]
                if k == 0 or k == n:
                    if diff != 0:
                        print(0)
                        return
                else:
                    if diff in seen or diff == 0:
                        print(0)
                        return
                    seen.add(diff)
    print(1)

if __name__ == '__main__':
    main()

