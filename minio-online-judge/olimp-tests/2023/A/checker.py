import sys
from collections import defaultdict

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f_input, open(submission_path) as f_submission:
        input_lines = f_input.read().splitlines()
        submission_lines = f_submission.read().splitlines()

    input_ptr = 0
    sub_ptr = 0
    t = int(input_lines[input_ptr])
    input_ptr += 1
    for _ in range(t):
        n = int(input_lines[input_ptr])
        input_ptr += 1
        input_arrays = []
        for _ in range(n):
            a, b = map(int, input_lines[input_ptr].split())
            input_arrays.append((a, b))
            input_ptr += 1

        if sub_ptr >= len(submission_lines):
            print(0)
            return
        submission_line = submission_lines[sub_ptr]
        sub_ptr += 1
        sub_nums = list(map(int, submission_line.split()))
        if len(sub_nums) != 2 * n:
            print(0)
            return
        submission_arrays = []
        for i in range(0, 2 * n, 2):
            x = sub_nums[i]
            y = sub_nums[i + 1]
            submission_arrays.append((x, y))

        # Check if submission uses the correct arrays (same multiset)
        input_counts = defaultdict(int)
        for pair in input_arrays:
            input_counts[pair] += 1
        sub_counts = defaultdict(int)
        for pair in submission_arrays:
            sub_counts[pair] += 1
        if input_counts != sub_counts:
            print(0)
            return

        # Check if sums are non-decreasing
        valid = True
        for i in range(len(submission_arrays) - 1):
            current_sum = submission_arrays[i][0] + submission_arrays[i][1]
            next_sum = submission_arrays[i + 1][0] + submission_arrays[i + 1][1]
            if current_sum > next_sum:
                valid = False
                break
        if not valid:
            print(0)
            return

    print(1)

if __name__ == "__main__":
    main()

