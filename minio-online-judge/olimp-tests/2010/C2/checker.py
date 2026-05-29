import sys

def check_yes_case(t, s_sub):
    len_t = len(t)
    len_s = len(s_sub)
    min_len_s = (len_t + 2) // 2
    if len_s < min_len_s or len_s > len_t - 1:
        return False
    k = 2 * len_s - len_t
    if not (1 <= k < len_s):
        return False
    expected_t = s_sub + s_sub[k:]
    return expected_t == t

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f:
        t = f.read().strip()

    with open(output_path) as f:
        ref_lines = [line.strip() for line in f.readlines() if line.strip()]

    with open(submission_path) as f:
        sub_lines = [line.strip() for line in f.readlines() if line.strip()]

    if not ref_lines:
        print(0)
        return

    if ref_lines[0] == "NO":
        if len(sub_lines) == 1 and sub_lines[0] == "NO":
            print(1)
        else:
            print(0)
        return
    else:
        if len(sub_lines) != 2 or sub_lines[0] != "YES":
            print(0)
            return
        s_sub = sub_lines[1]
        if not s_sub.isalpha() or not s_sub.islower():
            print(0)
            return
        if check_yes_case(t, s_sub):
            print(1)
        else:
            print(0)

if __name__ == "__main__":
    main()
