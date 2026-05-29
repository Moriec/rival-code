import sys


def main(input_path, output_path, submission_path):
    with open(input_path) as f:
        S = f.readline().strip()
        T = f.readline().strip()
    
    with open(output_path) as f:
        reference = f.readline().strip()
    
    with open(submission_path) as f:
        submission = f.readline().strip()
    
    if reference == '-1':
        print(1 if submission == '-1' else 0)
        return
    
    if submission == '-1':
        print(0)
        return
    
    if len(submission) != len(reference):
        print(0)
        return
    
    X = submission
    
    # Compute i_S (maximum prefix length)
    min_len = min(len(X), len(S))
    i_S = 0
    for i in range(min_len):
        if X[i] != S[i]:
            break
        i_S += 1
    
    max_i = min(i_S, len(X) - 1)
    if max_i < 1:
        print(0)
        return
    
    # Compute suffix hashes using reversed strings
    mod = 10**18 + 3
    base = 911382629
    
    def compute_hashes(s):
        rev_s = s[::-1]
        n = len(rev_s)
        prefix = [0] * (n + 1)
        power = [1] * (n + 1)
        for i in range(n):
            prefix[i+1] = (prefix[i] * base + ord(rev_s[i])) % mod
            power[i+1] = (power[i] * base) % mod
        return prefix, power
    
    X_prefix, X_power = compute_hashes(X)
    T_prefix, T_power = compute_hashes(T)
    
    count = 0
    len_X = len(X)
    len_T = len(T)
    
    for i in range(1, max_i + 1):
        if i >= len_X:
            continue
        l_prime = len_X - i
        if l_prime <= 0 or l_prime > len_T:
            continue
        if l_prime >= len(X_prefix) or l_prime >= len(T_prefix):
            continue
        hash_x = X_prefix[l_prime]
        hash_t = T_prefix[l_prime]
        if hash_x == hash_t:
            count += 1
            if count >= 2:
                print(1)
                return
    
    print(1 if count >= 2 else 0)


if __name__ == '__main__':
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]
    main(input_path, output_path, submission_path)
