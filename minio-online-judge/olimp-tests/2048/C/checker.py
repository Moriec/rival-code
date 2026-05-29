import sys

def readints(f):
    return list(map(int, f.readline().split()))

def main(input_path, output_path, submission_path):
    with open(input_path) as f_in, open(submission_path) as f_sub:
        t = int(f_in.readline())
        for _ in range(t):
            s = f_in.readline().strip()
            n = len(s)
            a = int(s, 2)
            if all(c == '1' for c in s):
                max_xor = a ^ 1
            else:
                p = None
                for i in range(n):
                    if s[i] == '0':
                        p = i + 1  # 1-based
                        break
                len_sub = n - p + 1
                max_xor = 0
                for start in range(n - len_sub + 1):
                    substr = s[start:start+len_sub]
                    b = int(substr, 2)
                    current_xor = a ^ b
                    if current_xor > max_xor:
                        max_xor = current_xor
            line = f_sub.readline().strip()
            while line == '':
                line = f_sub.readline().strip()
            l1, r1, l2, r2 = map(int, line.split())
            s1 = s[l1-1 : r1]
            s2 = s[l2-1 : r2]
            x = int(s1, 2)
            y = int(s2, 2)
            submission_xor = x ^ y
            if submission_xor != max_xor:
                print(0)
                return
    print(1)

if __name__ == "__main__":
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
