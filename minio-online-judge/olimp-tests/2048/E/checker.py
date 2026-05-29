import sys

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f_in, open(submission_path) as f_sub:
        t = int(f_in.readline())
        for _ in range(t):
            n, m = map(int, f_in.readline().split())
            sub_answer_line = f_sub.readline().strip().lower()
            expected_ans = 'yes' if m <= 2 * n - 1 else 'no'
            if sub_answer_line != expected_ans:
                print(0)
                return
            if expected_ans == 'yes':
                color_grid = []
                for _ in range(2 * n):
                    line = f_sub.readline()
                    if not line:
                        print(0)
                        return
                    parts = list(map(int, line.strip().split()))
                    if len(parts) != m:
                        print(0)
                        return
                    color_grid.append(parts)
                for row in color_grid:
                    for c in row:
                        if not (1 <= c <= n):
                            print(0)
                            return
                color_counts = [0] * (n + 1)
                total_edges = 0
                for row in color_grid:
                    for c in row:
                        color_counts[c] += 1
                        total_edges += 1
                if total_edges != 2 * n * m:
                    print(0)
                    return
                for c in range(1, n + 1):
                    if color_counts[c] > (2 * n + m - 1):
                        print(0)
                        return
        remaining = []
        for line in f_sub:
            if line.strip():
                remaining.append(line)
        if remaining:
            print(0)
            return
    print(1)

if __name__ == "__main__":
    main()
