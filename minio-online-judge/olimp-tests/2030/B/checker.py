import sys

def main(input_path, output_path, submission_path):
    with open(input_path) as f:
        input_lines = f.readlines()
    with open(submission_path) as f:
        submission_lines = [line.strip() for line in f.readlines()]

    t = int(input_lines[0])
    cases = input_lines[1:t+1]

    if len(submission_lines) != t:
        print(0)
        return

    for i in range(t):
        n = int(cases[i].strip())
        s = submission_lines[i]

        if len(s) != n:
            print(0)
            return

        if any(c not in {'0', '1'} for c in s):
            print(0)
            return

        c0 = s.count('0')

        if n == 1:
            if c0 not in {0, 1}:
                print(0)
                return
        else:
            if c0 != n - 1:
                print(0)
                return

    print(1)

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
