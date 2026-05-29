import sys

def main(input_path, output_path, submission_path):
    # Read input test cases
    with open(input_path) as f:
        input_lines = [line.strip() for line in f]
    input_ptr = 0
    t = int(input_lines[input_ptr])
    input_ptr +=1
    test_cases = []
    for _ in range(t):
        n, m = map(int, input_lines[input_ptr].split())
        input_ptr +=1
        a = list(map(int, input_lines[input_ptr].split()))
        input_ptr +=1
        b = list(map(int, input_lines[input_ptr].split()))
        input_ptr +=1
        test_cases.append( (n, m, a, b) )

    # Read reference output to see which test cases are possible
    with open(output_path) as f:
        ref_lines = [line.strip() for line in f if line.strip()]
    ref_ptr = 0
    ref_answers = []
    for _ in range(t):
        if ref_ptr >= len(ref_lines):
            ref_answers.append( (-1,) )
            continue
        line = ref_lines[ref_ptr]
        if line == '-1':
            ref_answers.append( (-1,) )
            ref_ptr +=1
        else:
            q = int(line)
            ref_answers.append( (q,) )
            ref_ptr +=1
            # Skip the operations in reference (we don't need them)
            ref_ptr += 2 * q

    # Read submission output
    with open(submission_path) as f:
        sub_lines = [line.strip() for line in f if line.strip()]
    sub_ptr = 0
    
    for case_idx in range(t):
        n, m, a, b = test_cases[case_idx]
        ref_ans = ref_answers[case_idx]
        
        if sub_ptr >= len(sub_lines):
            print(0)
            return
        current_sub_line = sub_lines[sub_ptr]
        
        if ref_ans[0] == -1:
            # Problem is impossible, submission must output -1
            if current_sub_line != '-1':
                print(0)
                return
            sub_ptr +=1
        else:
            # Problem is possible. submission must output a sequence.
            if current_sub_line == '-1':
                print(0)
                return
            try:
                q = int(current_sub_line)
            except:
                print(0)
                return
            if q <0 or q > (n + m):
                print(0)
                return
            sub_ptr +=1
            sum_k = 0
            current_array = a.copy()
            
            for _ in range(q):
                if sub_ptr >= len(sub_lines):
                    print(0)
                    return
                lr_k_line = sub_lines[sub_ptr].split()
                if len(lr_k_line) !=3:
                    print(0)
                    return
                try:
                    l = int(lr_k_line[0])
                    r = int(lr_k_line[1])
                    k = int(lr_k_line[2])
                except:
                    print(0)
                    return
                if k <1:
                    print(0)
                    return
                sum_k +=k
                if sum_k > (n + m):
                    print(0)
                    return
                sub_ptr +=1
                
                if sub_ptr >= len(sub_lines):
                    print(0)
                    return
                c_line = sub_lines[sub_ptr].split()
                if len(c_line) !=k:
                    print(0)
                    return
                try:
                    c = list(map(int, c_line))
                except:
                    print(0)
                    return
                for num in c:
                    if abs(num) > 1e9:
                        print(0)
                        return
                
                # Check l and r are valid in current_array
                if l <1 or r > len(current_array) or l > r:
                    print(0)
                    return
                
                # Apply replacement
                left = current_array[:l-1]
                right = current_array[r:]
                current_array = left + c + right
                sub_ptr +=1
            
            # After all operations, check sum_k <= n+m
            if sum_k > n + m:
                print(0)
                return
            
            # Check if current_array equals b
            if len(current_array) != m:
                print(0)
                return
            if current_array != b:
                print(0)
                return
    
    # All test cases passed
    print(1)

if __name__ == '__main__':
    import sys
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
