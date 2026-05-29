import sys

def main():
    input_path = sys.argv[1]
    submission_path = sys.argv[3]

    with open(input_path, 'r') as f:
        input_lines = f.read().splitlines()
    input_ptr = 0
    t = int(input_lines[input_ptr])
    input_ptr += 1

    with open(submission_path, 'r') as f:
        sub_lines = f.read().splitlines()
    sub_ptr = 0

    for _ in range(t):
        if input_ptr >= len(input_lines):
            print(0)
            return
        n = int(input_lines[input_ptr])
        input_ptr += 1
        a = list(map(int, input_lines[input_ptr].split()))
        input_ptr += 1
        original_sorted = sorted(a)

        if sub_ptr >= len(sub_lines):
            print(0)
            return
        try:
            k = int(sub_lines[sub_ptr])
        except:
            print(0)
            return
        sub_ptr += 1
        if k < 0 or k > 2 * n + 4:
            print(0)
            return
        paths = []
        valid = True
        for __ in range(k):
            if sub_ptr >= len(sub_lines):
                valid = False
                break
            s = sub_lines[sub_ptr].strip()
            sub_ptr += 1
            if len(s) != 2 * n - 2 or any(c not in {'R', 'D'} for c in s):
                valid = False
            paths.append(s)
        if not valid or len(paths) != k:
            print(0)
            return

        current_a = a.copy()
        for s in paths:
            x, y = 1, 1
            for step in s:
                if step == 'R':
                    y += 1
                else:
                    x += 1
                if x > n or y > n:
                    print(0)
                    return
            if x != n or y != n:
                print(0)
                return

            x_swap, y_swap = 1, 1
            temp_a = current_a.copy()
            for step in s:
                if step == 'R':
                    y_swap += 1
                else:
                    x_swap += 1
                if x_swap != y_swap:
                    temp_a[x_swap-1], temp_a[y_swap-1] = temp_a[y_swap-1], temp_a[x_swap-1]
            current_a = temp_a

        if current_a != original_sorted:
            print(0)
            return
    print(1)

if __name__ == '__main__':
    main()
