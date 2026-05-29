import sys

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    # Read correct output
    with open(output_path, 'r') as f:
        correct_line = f.read().strip()
        try:
            b = float(correct_line.split()[0])
        except:
            print(0)
            return
    
    # Read submission output
    with open(submission_path, 'r') as f:
        submission_lines = f.readlines()
        if not submission_lines:
            print(0)
            return
        submission_line = submission_lines[0].strip()
        parts = submission_line.split()
        if not parts:
            print(0)
            return
        submission_str = parts[0]
        try:
            a = float(submission_str)
        except ValueError:
            print(0)
            return
    
    # Calculate errors
    abs_error = abs(a - b)
    denominator = max(abs(b), 1.0)
    if abs_error <= 1e-6 * denominator:
        print(1)
    else:
        print(0)

if __name__ == "__main__":
    main()

