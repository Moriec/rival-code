import sys

def main(input_path, output_path, submission_path):
    with open(input_path) as f_in, open(submission_path) as f_sub:
        t = int(f_in.readline())
        for _ in range(t):
            l, r = map(int, f_in.readline().split())
            line = f_sub.readline().strip()
            if not line:
                print(0)
                return
            try:
                a, b, c = map(int, line.split())
            except:
                print(0)
                return
            
            # Check values are within [l, r]
            if not (l <= a <= r and l <= b <= r and l <= c <= r):
                print(0)
                return
            
            # Check distinct
            if len({a, b, c}) != 3:
                print(0)
                return
            
            # Compute XOR sum
            sum_xor = (a ^ b) + (b ^ c) + (a ^ c)
            
            # Compute maximum possible sum
            xor = l ^ r
            if xor == 0:
                max_sum = 0
            else:
                k = xor.bit_length() - 1
                max_sum = 2 * ((1 << (k + 1)) - 1)
            
            if sum_xor != max_sum:
                print(0)
                return
    
    print(1)

if __name__ == "__main__":
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
