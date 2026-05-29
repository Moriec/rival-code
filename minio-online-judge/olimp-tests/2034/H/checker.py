import sys
from math import gcd
from functools import reduce

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f_in, open(output_path) as f_ref, open(submission_path) as f_sub:
        t = int(f_in.readline())
        for _ in range(t):
            # Read input
            n = int(f_in.readline().strip())
            a = list(map(int, f_in.readline().strip().split()))
            original_set = set(a)

            # Read reference output
            ref_size_line = f_ref.readline().strip()
            if not ref_size_line:
                print(0)
                return
            ref_size = int(ref_size_line)
            f_ref.readline()  # Skip reference subset line

            # Read submission output
            sub_size_line = f_sub.readline().strip()
            if not sub_size_line:
                print(0)
                return
            try:
                sub_size = int(sub_size_line)
            except:
                print(0)
                return
            sub_elements_line = f_sub.readline().strip()
            if not sub_elements_line:
                print(0)
                return
            sub_elements = list(map(int, sub_elements_line.split()))

            # Check size match
            if sub_size != ref_size:
                print(0)
                return

            # Check subset validity
            if len(sub_elements) != sub_size:
                print(0)
                return
            if len(set(sub_elements)) != sub_size:
                print(0)
                return
            for elem in sub_elements:
                if elem not in original_set:
                    print(0)
                    return

            # Check linear independence
            if sub_size == 1:
                continue
            for i in range(sub_size):
                current = sub_elements[i]
                others = sub_elements[:i] + sub_elements[i+1:]
                current_gcd = reduce(gcd, others)
                if current % current_gcd == 0:
                    print(0)
                    return
    print(1)

if __name__ == "__main__":
    main()
