import sys
import math

def is_perfect_square(d):
    root = math.isqrt(d)
    return root * root == d

def main(input_path, output_path, submission_output_path):
    with open(input_path, 'r') as f:
        input_lines = f.read().splitlines()
    t = int(input_lines[0])
    test_cases = list(map(int, input_lines[1:t+1]))
    
    with open(output_path, 'r') as f:
        ref_lines = [line.strip() for line in f.read().splitlines()]
    
    with open(submission_output_path, 'r') as f:
        sub_lines = [line.strip() for line in f.read().splitlines()]
    
    if len(ref_lines) != t or len(sub_lines) != t:
        print(0)
        return
    
    for i in range(t):
        n = test_cases[i]
        ref_line = ref_lines[i]
        sub_line = sub_lines[i]
        
        if ref_line == '-1':
            if sub_line != '-1':
                print(0)
                return
            continue
        else:
            if sub_line == '-1':
                print(0)
                return
            try:
                parts = list(map(int, sub_line.split()))
            except:
                print(0)
                return
            if len(parts) != n:
                print(0)
                return
            
            count = {}
            for num in parts:
                count[num] = count.get(num, 0) + 1
            for num, cnt in count.items():
                if cnt == 1:
                    print(0)
                    return
            
            pos_dict = {}
            for idx, num in enumerate(parts):
                if num not in pos_dict:
                    pos_dict[num] = []
                else:
                    for p in pos_dict[num]:
                        distance = idx - p
                        if not is_perfect_square(distance):
                            print(0)
                            return
                pos_dict[num].append(idx)
    print(1)

if __name__ == "__main__":
    import sys
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_output_path = sys.argv[3]
    main(input_path, output_path, submission_output_path)
