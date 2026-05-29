import sys
from collections import Counter
import itertools

def readints(f):
    return list(map(int, f.readline().split()))

def main(input_path, output_path, submission_path):
    with open(input_path) as f_in, open(output_path) as f_ref, open(submission_path) as f_sub:
        t = int(f_in.readline())
        for _ in range(t):
            n = int(f_in.readline().strip())
            a = list(map(int, f_in.readline().split()))
            input_counter = Counter(a)
            
            # Read reference output
            ref_line1 = f_ref.readline().strip().lower()
            if ref_line1 == 'yes':
                ref_points = readints(f_ref)
                if len(ref_points) != 8:
                    print(0)
                    return
                ref_pairs = [(ref_points[i], ref_points[i+1]) for i in range(0,8,2)]
                ref_x = sorted(list({x for x, y in ref_pairs}))
                ref_y = sorted(list({y for x, y in ref_pairs}))
                ref_area = (ref_x[-1] - ref_x[0]) * (ref_y[-1] - ref_y[0])
            else:
                ref_area = None
            
            # Read submission output
            sub_line1 = f_sub.readline().strip().lower()
            if sub_line1 not in {'yes', 'no'}:
                print(0)
                return
            
            if sub_line1 == 'yes':
                try:
                    sub_points = list(map(int, f_sub.readline().split()))
                except:
                    print(0)
                    return
                if len(sub_points) != 8:
                    print(0)
                    return
                # Check element counts
                sub_counter = Counter(sub_points)
                for num, cnt in sub_counter.items():
                    if input_counter.get(num, 0) < cnt:
                        print(0)
                        return
                # Check valid rectangle
                sub_pairs = [(sub_points[i], sub_points[i+1]) for i in range(0,8,2)]
                sub_x = sorted(list({x for x, y in sub_pairs}))
                sub_y = sorted(list({y for x, y in sub_pairs}))
                required = list(itertools.product(sub_x, sub_y))
                sub_counter_pairs = Counter(sub_pairs)
                for p in required:
                    if sub_counter_pairs.get(p, 0) < 1:
                        print(0)
                        return
                # Compute area
                area = (sub_x[-1] - sub_x[0]) * (sub_y[-1] - sub_y[0])
                
                # Check against reference
                if ref_line1 != 'yes':
                    print(0)
                    return
                if area != ref_area:
                    print(0)
                    return
            else:
                if ref_line1 == 'yes':
                    print(0)
                    return
            
    print(1)

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3])
