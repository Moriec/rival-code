import sys
from collections import Counter

def is_valid_trapezoid(sticks):
    cnt = Counter(sticks)
    if len(cnt) == 1:
        return True
    pairs = [k for k, v in cnt.items() if v >= 2]
    if len(pairs) >= 2:
        return True
    if len(pairs) == 1:
        c = pairs[0]
        others = [k for k in cnt if k != c]
        if len(others) == 2:
            a, b = others
            return abs(a - b) < 2 * c
        elif len(others) == 1:
            return abs(c - others[0]) < 2 * c
    for k in cnt:
        if cnt[k] == 3:
            others = [x for x in sticks if x != k]
            if len(others) == 1:
                a = others[0]
                return abs(k - a) < 2 * k
    return False

def solution_exists(a):
    freq = Counter(a)
    if any(v >= 4 for v in freq.values()):
        return True
    pairs = [k for k, v in freq.items() if v >= 2]
    if len(pairs) >= 2:
        return True
    for candidate in pairs:
        remaining = []
        count = 0
        for num in a:
            if num == candidate:
                count += 1
                if count > 2:
                    remaining.append(num)
            else:
                remaining.append(num)
        remaining_sorted = sorted(remaining)
        for i in range(len(remaining_sorted) - 1):
            if abs(remaining_sorted[i] - remaining_sorted[i+1]) < 2 * candidate:
                return True
    return False

def main(input_path, output_path, submission_output_path):
    with open(input_path) as f_in, open(submission_output_path) as f_sub:
        input_lines = f_in.read().splitlines()
        submission_lines = f_sub.read().splitlines()
    input_ptr = 0
    t = int(input_lines[input_ptr])
    input_ptr += 1
    for _ in range(t):
        n = int(input_lines[input_ptr])
        input_ptr += 1
        a = list(map(int, input_lines[input_ptr].split()))
        input_ptr += 1
        if not submission_lines:
            print(0)
            return
        sub_line = submission_lines.pop(0).strip()
        if sub_line == '-1':
            if solution_exists(a):
                print(0)
                return
        else:
            try:
                submitted = list(map(int, sub_line.split()))
            except:
                print(0)
                return
            if len(submitted) != 4:
                print(0)
                return
            input_counts = Counter(a)
            sub_counts = Counter(submitted)
            for k, v in sub_counts.items():
                if input_counts.get(k, 0) < v:
                    print(0)
                    return
            if not is_valid_trapezoid(submitted):
                print(0)
                return
    print(1)

if __name__ == '__main__':
    import sys
    main(sys.argv[1], sys.argv[2], sys.argv[3])
