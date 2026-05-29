import sys

def main(input_path, output_path, submission_path):
    with open(input_path) as f_in, open(submission_path) as f_sub:
        t = int(f_in.readline())
        for _ in range(t):
            n = int(f_in.readline().strip())
            a = list(map(int, f_in.readline().split()))
            original_a = a.copy()
            try:
                k_line = f_sub.readline().strip()
                if not k_line:
                    print(0)
                    return
                k = int(k_line)
            except ValueError:
                print(0)
                return
            if k < 0 or k > n + 4:
                print(0)
                return
            paths = []
            for _ in range(k):
                line = f_sub.readline().strip()
                if not line:
                    print(0)
                    return
                paths.append(line)
            current_a = a.copy()
            for path in paths:
                if len(path) != 2 * n - 2:
                    print(0)
                    return
                if any(c not in {'R', 'D'} for c in path):
                    print(0)
                    return
                x, y = 1, 1
                for c in path:
                    if c == 'R':
                        y += 1
                    else:
                        x += 1
                    if x > n or y > n:
                        print(0)
                        return
                    if x != y:
                        current_a[x-1], current_a[y-1] = current_a[y-1], current_a[x-1]
            if current_a != sorted(original_a):
                print(0)
                return
    print(1)

if __name__ == "__main__":
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
