import sys
from collections import Counter

def main():
    input_path = sys.argv[1]
    output_path = sys.argv[2]
    submission_path = sys.argv[3]

    with open(input_path) as f_input, open(output_path) as f_output, open(submission_path) as f_sub:
        input_lines = f_input.read().splitlines()
        output_lines = f_output.read().splitlines()
        sub_lines = f_sub.read().splitlines()

    ptr_input = 0
    ptr_output = 0
    ptr_sub = 0
    t = int(input_lines[ptr_input])
    ptr_input += 1

    for _ in range(t):
        # Read input
        n = int(input_lines[ptr_input])
        ptr_input += 1
        intervals = []
        for __ in range(n):
            l, r = map(int, input_lines[ptr_input].split())
            intervals.append((l, r))
            ptr_input += 1

        # Read reference output (k)
        k_ref = int(output_lines[ptr_output])
        ptr_output += 1
        # Skip colors in reference
        ptr_output += 1

        # Read submission output
        if ptr_sub >= len(sub_lines):
            print(0)
            return
        k_sub = int(sub_lines[ptr_sub])
        ptr_sub += 1
        if ptr_sub >= len(sub_lines):
            print(0)
            return
        colors = list(map(int, sub_lines[ptr_sub].split()))
        ptr_sub += 1

        # Check k
        if k_sub != k_ref:
            print(0)
            return

        # Check colors are valid
        if len(colors) != n or any(c < 1 or c > k_sub for c in colors):
            print(0)
            return

        # Generate events
        events = []
        for i in range(n):
            l, r = intervals[i]
            events.append((l, 1, i))  # start
            events.append((r + 1, 0, i))  # end

        # Sort events: time asc, end before start
        events.sort(key=lambda x: (x[0], x[1], x[2]))

        active = set()
        prev_time = None
        i = 0
        while i < len(events):
            current_time = events[i][0]
            # Process all events at current_time
            j = i
            while j < len(events) and events[j][0] == current_time:
                j += 1

            # Update active set
            for k in range(i, j):
                typ, idx = events[k][1], events[k][2]
                if typ == 0:
                    if idx in active:
                        active.remove(idx)
                else:
                    active.add(idx)

            # Check interval [prev_time, current_time -1]
            if prev_time is not None:
                if active:
                    current_colors = [colors[idx] for idx in active]
                    cnt = Counter(current_colors)
                    if not any(v == 1 for v in cnt.values()):
                        print(0)
                        return

            prev_time = current_time
            i = j

    print(100)

if __name__ == "__main__":
    main()
