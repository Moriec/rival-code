import sys

def compute_mex(arr):
    s = set(arr)
    mex = 0
    while mex in s:
        mex += 1
    return mex

def main(input_path, output_path, submission_path):
    with open(input_path, 'r') as f:
        input_lines = f.read().splitlines()
    with open(submission_path, 'r') as f:
        submission_lines = f.read().splitlines()
    
    t = int(input_lines[0])
    input_ptr = 1
    submission_ptr = 0
    
    for _ in range(t):
        n, x, y = map(int, input_lines[input_ptr].split())
        input_ptr += 1
        
        if submission_ptr >= len(submission_lines):
            print(0)
            return
        
        submission_line = submission_lines[submission_ptr].strip()
        submission_ptr += 1
        a = list(map(int, submission_line.split()))
        
        if len(a) != n:
            print(0)
            return
        
        for i in range(1, n + 1):
            prev = i - 1 if i > 1 else n
            next_i = i + 1 if i < n else 1
            friends = {prev, next_i}
            
            if i == x:
                friends.add(y)
            elif i == y:
                friends.add(x)
            
            friend_vals = [a[f - 1] for f in friends]
            mex = compute_mex(friend_vals)
            
            if a[i - 1] != mex:
                print(0)
                return
    
    print(1)

if __name__ == "__main__":
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
