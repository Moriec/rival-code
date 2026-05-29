import sys

def read_file(path):
    with open(path, 'r') as f:
        lines = [line.strip() for line in f.readlines()]
    return lines

def main(input_path, output_path, submission_path):
    # Read input to get R and C
    input_lines = read_file(input_path)
    R, C = map(int, input_lines[0].split())
    
    # Read reference output
    ref_lines = read_file(output_path)
    if not ref_lines:
        return 0
    try:
        k_ref = int(ref_lines[0])
    except:
        return 0
    ref_positions = []
    if k_ref > 0:
        if len(ref_lines) < 2:
            return 0
        ref_positions = ref_lines[1].split()
        if len(ref_positions) != k_ref:
            return 0
    
    # Read submission output
    sub_lines = read_file(submission_path)
    if not sub_lines:
        return 0
    try:
        k_sub = int(sub_lines[0])
    except:
        return 0
    if k_sub != k_ref:
        return 0
    sub_positions = []
    if k_sub > 0:
        if len(sub_lines) < 2:
            return 0
        sub_positions = sub_lines[1].split()
        if len(sub_positions) != k_sub:
            return 0
    else:
        if len(sub_lines) != 1:
            return 0
    
    # Check submission positions are valid
    valid_directions = {'N', 'S', 'E', 'W'}
    for pos in sub_positions:
        if len(pos) < 1:
            return 0
        direction = pos[0]
        if direction not in valid_directions:
            return 0
        num_str = pos[1:]
        if not num_str.isdigit():
            return 0
        num = int(num_str)
        if direction in ['N', 'S']:
            if not (1 <= num <= C):
                return 0
        else:
            if not (1 <= num <= R):
                return 0
    
    # Check that the submission's positions are a permutation of reference's
    if k_ref > 0:
        if set(sub_positions) != set(ref_positions):
            return 0
    
    # All checks passed
    return 1

if __name__ == '__main__':
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    score = main(input_path, output_path, submission_path)
    print(score)
