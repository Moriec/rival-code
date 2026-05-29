import sys

def main(input_path, output_path, submission_path):
    with open(input_path) as f_in, open(submission_path) as f_sub:
        sub_lines = []
        for line in f_sub:
            stripped = line.strip()
            if stripped:
                sub_lines.append(stripped)
        ptr = 0
        t = int(f_in.readline())
        for _ in range(t):
            n, k = map(int, f_in.readline().split())
            if ptr >= len(sub_lines):
                print(0)
                return
            line1 = sub_lines[ptr]
            ptr += 1
            if line1 == '-1':
                solution_possible = (n == 1 and k == 1) or (n > 1 and k not in (1, n))
                if solution_possible:
                    print(0)
                    return
                continue
            else:
                try:
                    m = int(line1)
                except:
                    print(0)
                    return
                if m % 2 == 0 or m < 1 or m > n:
                    print(0)
                    return
                if ptr >= len(sub_lines):
                    print(0)
                    return
                line2 = sub_lines[ptr]
                ptr += 1
                p = list(map(int, line2.split()))
                if len(p) != m:
                    print(0)
                    return
                if p[0] != 1:
                    print(0)
                    return
                for i in range(1, m):
                    if p[i] <= p[i-1]:
                        print(0)
                        return
                if p[-1] > n:
                    print(0)
                    return
                medians = []
                for i in range(m):
                    if i < m - 1:
                        start = p[i]
                        end = p[i+1] - 1
                    else:
                        start = p[i]
                        end = n
                    length = end - start + 1
                    if length % 2 == 0:
                        print(0)
                        return
                    median = (start + end) // 2
                    medians.append(median)
                sorted_medians = sorted(medians)
                mid_idx = m // 2
                if sorted_medians[mid_idx] != k:
                    print(0)
                    return
        if ptr < len(sub_lines):
            print(0)
            return
        print(1)

if __name__ == '__main__':
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
