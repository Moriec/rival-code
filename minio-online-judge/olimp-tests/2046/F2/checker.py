import sys

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f_in, open(output_path) as f_out, open(submission_path) as f_sub:
        t = int(f_in.readline().strip())
        ref_lines = [line.rstrip('\n') for line in f_out]
        sub_lines = [line.rstrip('\n') for line in f_sub]

        ref_ptr = 0
        sub_ptr = 0
        for _ in range(t):
            # Read input template
            template = f_in.readline().strip()
            n_template = len(template)

            # Process reference output
            if ref_ptr >= len(ref_lines):
                print(0)
                return
            ref_ans = ref_lines[ref_ptr]
            ref_ptr += 1
            if ref_ans == 'YES':
                if ref_ptr >= len(ref_lines):
                    print(0)
                    return
                ref_str = ref_lines[ref_ptr]
                ref_ptr += 1
                # Skip insertion steps in reference
                while ref_ptr < len(ref_lines) and ref_lines[ref_ptr] not in {'YES', 'NO'}:
                    ref_ptr += 1

            # Process submission output
            if sub_ptr >= len(sub_lines):
                print(0)
                return
            sub_ans = sub_lines[sub_ptr]
            sub_ptr += 1

            # Check if answers match
            if sub_ans != ref_ans:
                print(0)
                return
            if sub_ans == 'NO':
                continue

            # Check submission's constructed string
            if sub_ptr >= len(sub_lines):
                print(0)
                return
            constructed = sub_lines[sub_ptr]
            sub_ptr += 1

            # Check constructed length matches template
            if len(constructed) != n_template:
                print(0)
                return
            # Check characters match template
            for sc, tc in zip(constructed, template):
                if tc != '?' and sc != tc:
                    print(0)
                    return

            # Read insertion steps tokens
            required_tokens = 2 * len(constructed)
            tokens = []
            while len(tokens) < required_tokens:
                if sub_ptr >= len(sub_lines):
                    print(0)
                    return
                line = sub_lines[sub_ptr]
                sub_ptr += 1
                tokens.extend(line.split())

            if len(tokens) != required_tokens:
                print(0)
                return

            # Parse pairs and triples
            try:
                pairs = list(zip(tokens[::2], tokens[1::2]))
            except IndexError:
                print(0)
                return

            n = len(constructed)
            num_triples = n // 3
            if len(pairs) != 3 * num_triples:
                print(0)
                return
            triples = [pairs[i*3 : (i+1)*3] for i in range(num_triples)]

            # Check each triple has Y, D, X exactly once
            for triple in triples:
                chars = {c for c, _ in triple}
                if chars != {'Y', 'D', 'X'}:
                    print(0)
                    return

            # Simulate insertions
            current = ''
            for triple in triples:
                temp = current
                for c, p_str in triple:
                    try:
                        p = int(p_str)
                    except ValueError:
                        print(0)
                        return
                    if p < 0 or p > len(temp):
                        print(0)
                        return
                    temp = temp[:p] + c + temp[p:]
                # Check adjacent duplicates
                for i in range(len(temp)-1):
                    if temp[i] == temp[i+1]:
                        print(0)
                        return
                current = temp

            if current != constructed:
                print(0)
                return

    print(1)

if __name__ == '__main__':
    main()
