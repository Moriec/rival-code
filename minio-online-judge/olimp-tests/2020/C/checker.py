import sys

def check_possible(b, c, d):
    for bit in range(61):
        mask = 1 << bit
        b_bit = (b & mask) != 0
        c_bit = (c & mask) != 0
        d_bit = (d & mask) != 0
        if (b_bit, c_bit, d_bit) == (True, False, False):
            return False
        if (b_bit, c_bit, d_bit) == (False, True, True):
            return False
    return True

def main():
    input_path = sys.argv[1]
    submission_path = sys.argv[3]

    with open(input_path) as f_in, open(submission_path) as f_sub:
        t = int(f_in.readline().strip())
        submission_lines = [line.strip() for line in f_sub.readlines()]
        if len(submission_lines) != t:
            print(0)
            return
        for i in range(t):
            bcd_line = f_in.readline().strip()
            if not bcd_line:
                print(0)
                return
            b, c, d = map(int, bcd_line.split())
            a_line = submission_lines[i]
            if a_line == '-1':
                if check_possible(b, c, d):
                    print(0)
                    return
            else:
                try:
                    a = int(a_line)
                except ValueError:
                    print(0)
                    return
                if a < 0 or a > (1 << 61):
                    print(0)
                    return
                if not check_possible(b, c, d):
                    print(0)
                    return
                res = (a | b) - (a & c)
                if res != d:
                    print(0)
                    return
    print(1)

if __name__ == "__main__":
    main()
