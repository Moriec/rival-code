import sys

def highest_bit(n):
    return 1 << (n.bit_length() - 1)

def main(input_path, output_path, submission_path):
    with open(input_path) as inf, open(submission_path) as subf:
        t = int(inf.readline())
        for _ in range(t):
            n = int(inf.readline().strip())
            if n % 2 == 1:
                correct_max_k = n
            else:
                h = highest_bit(n)
                correct_max_k = h * 2 - 1
            
            try:
                sub_max_k = int(subf.readline().strip())
                perm = list(map(int, subf.readline().split()))
            except:
                print(0)
                return
            
            if len(perm) != n or sorted(perm) != list(range(1, n+1)) or sub_max_k != correct_max_k:
                print(0)
                return
            
            k = 0
            for i in range(n):
                op_num = i + 1
                p = perm[i]
                if op_num % 2 == 1:
                    k &= p
                else:
                    k |= p
            
            if k != sub_max_k:
                print(0)
                return
        print(1)

if __name__ == "__main__":
    input_path, output_path, submission_path = sys.argv[1], sys.argv[2], sys.argv[3]
    main(input_path, output_path, submission_path)
