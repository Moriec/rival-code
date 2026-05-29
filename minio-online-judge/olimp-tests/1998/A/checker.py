import sys

def main():
    input_path = sys.argv[1]
    ref_output_path = sys.argv[2]  # Not used
    submission_output_path = sys.argv[3]

    with open(input_path) as f:
        input_lines = f.read().splitlines()
    with open(submission_output_path) as f:
        sub_lines = [line.strip() for line in f]

    t = int(input_lines[0])
    ptr = 1
    sub_ptr = 0

    for _ in range(t):
        if ptr >= len(input_lines):
            print(0)
            return
        xc, yc, k = map(int, input_lines[ptr].split())
        ptr += 1

        points = []
        for __ in range(k):
            if sub_ptr >= len(sub_lines):
                print(0)
                return
            line = sub_lines[sub_ptr]
            sub_ptr +=1
            parts = line.split()
            if len(parts)!=2:
                print(0)
                return
            try:
                x = int(parts[0])
                y = int(parts[1])
            except:
                print(0)
                return
            if not (-1e9 <= x <= 1e9 and -1e9 <= y <= 1e9):
                print(0)
                return
            points.append((x, y))

        if len(set(points)) != k:
            print(0)
            return

        sum_x = sum(x for x, y in points)
        sum_y = sum(y for x, y in points)
        if sum_x != xc * k or sum_y != yc * k:
            print(0)
            return

    if sub_ptr != len(sub_lines):
        print(0)
        return

    print(1)

if __name__ == "__main__":
    main()
