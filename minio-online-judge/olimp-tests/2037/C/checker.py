import sys

def main():
    # Precompute sieve up to 4e5+1
    max_sieve = 4 * 10**5 + 1
    sieve = [True] * (max_sieve + 1)
    sieve[0] = sieve[1] = False
    for i in range(2, int(max_sieve**0.5) + 1):
        if sieve[i]:
            sieve[i*i : max_sieve+1 : i] = [False] * len(sieve[i*i : max_sieve+1 : i])

    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    # Read input data
    with open(input_path, 'r') as f:
        input_lines = f.read().splitlines()
    t = int(input_lines[0])
    test_cases = [int(line.strip()) for line in input_lines[1:t+1]]

    # Read correct outputs
    with open(output_path, 'r') as f:
        correct_lines = [line.strip() for line in f.read().splitlines()]

    # Read submission outputs
    with open(submission_path, 'r') as f:
        submission_lines = [line.strip() for line in f.read().splitlines()]

    # Check if the number of test cases matches
    if len(test_cases) != t or len(correct_lines) != t or len(submission_lines) != t:
        print(0)
        return

    overall_score = 1
    for i in range(t):
        n = test_cases[i]
        correct = correct_lines[i]
        submission = submission_lines[i]

        # Check if the correct output is -1
        if correct == '-1':
            if submission != '-1':
                overall_score = 0
            continue
        else:
            # Correct output is a permutation, so submission must be a valid permutation with composite sums
            if submission == '-1':
                overall_score = 0
                continue
            # Parse submission line into integers
            try:
                p = list(map(int, submission.split()))
            except:
                # Invalid format
                overall_score = 0
                continue
            # Check length
            if len(p) != n:
                overall_score = 0
                continue
            # Check if it's a permutation of 1..n
            if sorted(p) != list(range(1, n+1)):
                overall_score = 0
                continue
            # Check all sums are composite
            valid = True
            for j in range(n-1):
                s = p[j] + p[j+1]
                if sieve[s]:
                    # Sum is a prime → invalid
                    valid = False
                    break
            if not valid:
                overall_score = 0

    print(overall_score)

if __name__ == "__main__":
    main()
