import sys
from bisect import bisect_right

def main(input_path, output_path, submission_path):
    with open(input_path) as f_input, open(output_path) as f_output, open(submission_path) as f_submission:
        input_lines = f_input.read().splitlines()
        ref_lines = f_output.read().splitlines()
        sub_lines = f_submission.read().splitlines()

        input_ptr = 0
        ref_ptr = 0
        sub_ptr = 0

        T = int(input_lines[input_ptr])
        input_ptr += 1

        for _ in range(T):
            n, m = map(int, input_lines[input_ptr].split())
            input_ptr += 1

            participants = []
            for _ in range(n):
                a_i, b_i, s_i = map(int, input_lines[input_ptr].split())
                participants.append((a_i, b_i, s_i))
                input_ptr += 1

            cities = []
            for _ in range(m):
                parts = list(map(int, input_lines[input_ptr].split()))
                cities.append(parts[1:])
                input_ptr += 1

            # Read reference output
            ref_is_possible = True
            if ref_ptr >= len(ref_lines):
                print(0)
                return
            ref_first = ref_lines[ref_ptr].strip()
            if ref_first == '-1':
                ref_is_possible = False
                ref_ptr += 1
            else:
                ref_p = int(ref_first)
                ref_ptr += 1
                ref_problems = []
                for _ in range(ref_p):
                    if ref_ptr >= len(ref_lines):
                        print(0)
                        return
                    d, t = map(int, ref_lines[ref_ptr].split())
                    ref_problems.append((d, t))
                    ref_ptr += 1

            # Read submission output
            if sub_ptr >= len(sub_lines):
                print(0)
                return
            sub_first = sub_lines[sub_ptr].strip()
            if sub_first == '-1':
                sub_is_possible = False
                sub_ptr += 1
            else:
                try:
                    sub_p = int(sub_first)
                except:
                    print(0)
                    return
                sub_ptr += 1
                sub_problems = []
                for _ in range(sub_p):
                    if sub_ptr >= len(sub_lines):
                        print(0)
                        return
                    parts = sub_lines[sub_ptr].split()
                    if len(parts) != 2:
                        print(0)
                        return
                    try:
                        d = int(parts[0])
                        t = int(parts[1])
                    except:
                        print(0)
                        return
                    sub_problems.append((d, t))
                    sub_ptr += 1
                sub_is_possible = True

            # Check if submission matches reference possibility
            if not ref_is_possible:
                if sub_is_possible:
                    print(0)
                    return
                else:
                    continue
            else:
                if not sub_is_possible:
                    print(0)
                    return

                # Validate submission's problems
                if sub_p < 1 or sub_p > 5 * n:
                    print(0)
                    return

                topics = set()
                for d, t in sub_problems:
                    if t in topics:
                        print(0)
                        return
                    topics.add(t)
                    if d < 0 or d > 1e9 or t < 0 or t > 1e9:
                        print(0)
                        return

                sorted_d = sorted(d for d, t in sub_problems)
                topic_to_d = {t: d for d, t in sub_problems}

                city1 = set(cities[0])
                city2 = set(cities[1])

                min_c1 = float('inf')
                max_c2 = -float('inf')

                for idx in range(n):
                    a, b, s = participants[idx]
                    count1 = bisect_right(sorted_d, a)
                    count2 = 0
                    if s in topic_to_d:
                        d_t = topic_to_d[s]
                        if a < d_t <= b:
                            count2 = 1
                    total = count1 + count2

                    pid = idx + 1  # participant index is 1-based
                    if pid in city1:
                        if total < min_c1:
                            min_c1 = total
                    elif pid in city2:
                        if total > max_c2:
                            max_c2 = total

                if min_c1 > max_c2:
                    continue
                else:
                    print(0)
                    return

        print(1)

if __name__ == '__main__':
    import sys
    main(sys.argv[1], sys.argv[2], sys.argv[3])
