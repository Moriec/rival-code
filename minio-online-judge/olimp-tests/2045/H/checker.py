import sys

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    # Read input S
    with open(input_path) as f:
        S = f.read().strip()

    # Read reference output
    with open(output_path) as f:
        ref_lines = [line.rstrip('\n') for line in f.readlines()]
    if len(ref_lines) < 1:
        print(0)
        return
    try:
        n_ref = int(ref_lines[0])
    except:
        print(0)
        return
    ref_words = ref_lines[1:1 + n_ref]
    if len(ref_words) != n_ref:
        print(0)
        return

    # Read submission output
    with open(submission_path) as f:
        sub_lines = [line.rstrip('\n') for line in f.readlines()]
    if len(sub_lines) < 1:
        print(0)
        return
    try:
        n_sub = int(sub_lines[0])
    except:
        print(0)
        return
    if len(sub_lines) != n_sub + 1:
        print(0)
        return
    sub_words = sub_lines[1:]

    # Check n matches
    if n_sub != n_ref:
        print(0)
        return

    # Check all words are uppercase letters and non-empty
    for word in sub_words:
        if not word.isalpha() or not word.isupper() or len(word) == 0:
            print(0)
            return

    # Check distinct
    if len(set(sub_words)) != len(sub_words):
        print(0)
        return

    # Check sorted
    for i in range(len(sub_words) - 1):
        if sub_words[i] > sub_words[i + 1]:
            print(0)
            return

    # Check concatenation
    if ''.join(sub_words) != S:
        print(0)
        return

    # All checks passed
    print(1)

if __name__ == "__main__":
    main()
