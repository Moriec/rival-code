import sys

def readints(line):
    return list(map(int, line.strip().split()))

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f:
        input_lines = [line.strip() for line in f]

    with open(submission_path) as f:
        sub_lines = [line.strip() for line in f]

    with open(output_path) as f:
        ref_lines = [line.strip() for line in f]

    ptr_input = 0
    T = int(input_lines[ptr_input])
    ptr_input += 1

    ptr_sub = 0
    ptr_ref = 0

    for _ in range(T):
        n, m = map(int, input_lines[ptr_input].split())
        ptr_input += 1

        participants = []
        for _ in range(n):
            a, b, s = map(int, input_lines[ptr_input].split())
            participants.append((a, b, s))
            ptr_input += 1

        cities = []
        for _ in range(m):
            parts = list(map(int, input_lines[ptr_input].split()))
            k_i = parts[0]
            qs = [x - 1 for x in parts[1:]]
            cities.append(qs)
            ptr_input += 1

        if ptr_sub >= len(sub_lines):
            print(0)
            return
        sub_first = sub_lines[ptr_sub]

        if sub_first == '-1':
            if ptr_ref >= len(ref_lines) or ref_lines[ptr_ref] != '-1':
                print(0)
                return
            ptr_sub += 1
            ptr_ref += 1
            continue

        try:
            p = int(sub_first)
        except:
            print(0)
            return
        if not (1 <= p <= 5 * n):
            print(0)
            return

        if ptr_sub + 1 + p > len(sub_lines):
            print(0)
            return
        problems = []
        topics = set()
        for i in range(ptr_sub + 1, ptr_sub + 1 + p):
            line = sub_lines[i]
            parts = line.split()
            if len(parts) != 2:
                print(0)
                return
            try:
                d = int(parts[0])
                t = int(parts[1])
            except:
                print(0)
                return
            if not (0 <= d <= 1e9 and 0 <= t <= 1e9):
                print(0)
                return
            if t in topics:
                print(0)
                return
            topics.add(t)
            problems.append((d, t))

        counts = [0] * n
        for d, t in problems:
            for i in range(n):
                a, b, s = participants[i]
                if a >= d or (s == t and b >= d):
                    counts[i] += 1

        city_counts = []
        for city in cities:
            city_counts.append([counts[q] for q in city])

        for i in range(m):
            for j in range(i + 1, m):
                min_i = min(city_counts[i])
                max_j = max(city_counts[j])
                if min_i <= max_j:
                    print(0)
                    return

        ptr_sub += 1 + p

        if ptr_ref >= len(ref_lines) or ref_lines[ptr_ref] == '-1':
            print(0)
            return
        ref_p = int(ref_lines[ptr_ref])
        ptr_ref += 1 + ref_p

    print(1)

if __name__ == "__main__":
    main()
