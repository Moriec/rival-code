import sys

def read_float_from_file(path):
    try:
        with open(path, 'r') as f:
            line = f.readline().strip()
            return float(line)
    except (ValueError, IOError):
        return None

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    correct = read_float_from_file(output_path)
    submission = read_float_from_file(submission_path)

    if correct is None or submission is None:
        print(0)
        return

    a = submission
    b = correct

    absolute_error = abs(a - b)
    if absolute_error <= 1e-4:
        print(1)
        return

    max_denominator = max(1.0, abs(b))
    relative_error = absolute_error / max_denominator

    if relative_error <= 1e-4:
        print(1)
    else:
        print(0)

if __name__ == "__main__":
    main()
