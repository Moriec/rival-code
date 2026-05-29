import sys

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f_in, open(submission_path) as f_sub:
        input_lines = f_in.read().splitlines()
        sub_lines = f_sub.read().splitlines()

    input_ptr = 0
    t = int(input_lines[input_ptr])
    input_ptr += 1
    sub_ptr = 0

    for _ in range(t):
        if input_ptr >= len(input_lines):
            print(0)
            return
        n, m = map(int, input_lines[input_ptr].split())
        input_ptr += 1

        original = []
        for _ in range(n):
            if input_ptr >= len(input_lines):
                print(0)
                return
            original.append(input_lines[input_ptr].strip())
            input_ptr += 1

        submission = []
        for _ in range(n):
            if sub_ptr >= len(sub_lines):
                print(0)
                return
            line = sub_lines[sub_ptr].strip()
            if len(line) != m:
                print(0)
                return
            submission.append(line)
            sub_ptr += 1

        # Validate submission format and collect free cells and S
        free_cells = set()
        s = 0
        for i in range(n):
            orig_line = original[i]
            sub_line = submission[i]
            for j in range(m):
                orig_char = orig_line[j]
                sub_char = sub_line[j]
                if orig_char == '.':
                    if sub_char != '.':
                        print(0)
                        return
                else:
                    if sub_char not in ('#', 'S'):
                        print(0)
                        return
                    if orig_char == '#':
                        free_cells.add((i, j))
                        s += 1

        # Calculate perimeter p
        p = 0
        dirs = [(-1, 0), (1, 0), (0, -1), (0, 1)]
        for (i, j) in free_cells:
            for dx, dy in dirs:
                ni, nj = i + dx, j + dy
                if ni < 0 or ni >= n or nj < 0 or nj >= m:
                    p += 1
                else:
                    if original[ni][nj] != '#':
                        p += 1

        # Collect S set
        S = set()
        for i in range(n):
            for j in range(m):
                if submission[i][j] == 'S':
                    S.add((i, j))

        if len(S) > (s + p) / 5.0 + 1e-9:
            print(0)
            return

        # Check coverage
        for (i, j) in free_cells:
            if (i, j) in S:
                continue
            covered = False
            for dx, dy in dirs:
                ni, nj = i + dx, j + dy
                if (ni, nj) in S:
                    covered = True
                    break
            if not covered:
                print(0)
                return

    if sub_ptr != len(sub_lines):
        print(0)
        return

    print(1)

if __name__ == "__main__":
    main()
