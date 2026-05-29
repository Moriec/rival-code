import sys

def main(input_path, output_path, submission_path):
    with open(input_path) as f_in, open(output_path) as f_ref, open(submission_path) as f_sub:
        t = int(f_in.readline())
        for _ in range(t):
            n = int(f_in.readline().strip())
            cities = []
            for _ in range(n):
                x, y = map(int, f_in.readline().split())
                cities.append((x, y))
            # Read reference output
            ref_k = int(f_ref.readline().strip())
            ref_x, ref_y = map(int, f_ref.readline().split())
            # Read submission output
            try:
                sub_k = int(f_sub.readline().strip())
            except:
                print(0)
                return
            try:
                sub_x, sub_y = map(int, f_sub.readline().split())
            except:
                print(0)
                return
            # Check k matches reference
            if sub_k != ref_k:
                print(0)
                return
            # Compute regions for submission's point
            count1 = 0
            count2 = 0
            count3 = 0
            count4 = 0
            for x, y in cities:
                if x >= sub_x and y >= sub_y:
                    count1 +=1
                elif x < sub_x and y >= sub_y:
                    count2 +=1
                elif x >= sub_x and y < sub_y:
                    count3 +=1
                else:
                    count4 +=1
            min_count = min(count1, count2, count3, count4)
            if min_count < ref_k:
                print(0)
                return
    print(1)
    return

if __name__ == "__main__":
    import sys
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
