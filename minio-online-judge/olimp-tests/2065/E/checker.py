import sys

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f:
        t = int(f.readline())
        test_cases = [tuple(map(int, f.readline().split())) for _ in range(t)]

    with open(submission_path) as f:
        submission_output = [line.strip() for line in f.readlines()]

    if len(submission_output) != t:
        print(0)
        return

    for i in range(t):
        n, m, k = test_cases[i]
        s = submission_output[i]

        total_diff = abs(n - m)
        max_run = max(n, m)
        possible = (k >= total_diff) and (k <= max_run)

        if s == '-1':
            if not possible:
                continue
            else:
                print(0)
                return
        else:
            if not possible:
                print(0)
                return

            if len(s) != n + m:
                print(0)
                return

            cnt0 = s.count('0')
            cnt1 = s.count('1')

            if cnt0 != n or cnt1 != m:
                print(0)
                return

            arr = [1 if c == '0' else -1 for c in s]
            if not arr:
                max_balance = 0
            else:
                max_sub = current_max = arr[0]
                min_sub = current_min = arr[0]
                for num in arr[1:]:
                    current_max = max(num, current_max + num)
                    max_sub = max(max_sub, current_max)
                    current_min = min(num, current_min + num)
                    min_sub = min(min_sub, current_min)
                max_balance = max(max_sub, -min_sub)

            if max_balance != k:
                print(0)
                return

    print(1)

if __name__ == "__main__":
    main()

