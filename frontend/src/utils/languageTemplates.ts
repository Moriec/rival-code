import type { SupportedUiLanguage } from '../types/contracts';

export function defaultCode(language: SupportedUiLanguage): string {
  switch (language) {
    case 'CPP':
      return '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n\n    return 0;\n}\n';
    case 'PYTHON':
      return 'import sys\n\n\ndef main():\n    data = sys.stdin.read().strip().split()\n\n\nif __name__ == "__main__":\n    main()\n';
    case 'RUST':
      return 'use std::io::{self, Read};\n\nfn main() {\n    let mut input = String::new();\n    io::stdin().read_to_string(&mut input).unwrap();\n}\n';
    case 'JAVASCRIPT':
      return 'const fs = require("fs");\nconst input = fs.readFileSync(0, "utf8").trim().split(/\\s+/);\n\n';
    case 'JAVA':
    default:
      return 'import java.io.*;\nimport java.util.*;\n\npublic class Main {\n    public static void main(String[] args) throws Exception {\n        FastScanner fs = new FastScanner(System.in);\n    }\n\n    static class FastScanner {\n        private final InputStream in;\n        private final byte[] buffer = new byte[1 << 16];\n        private int ptr = 0, len = 0;\n\n        FastScanner(InputStream in) { this.in = in; }\n\n        String next() throws IOException {\n            StringBuilder sb = new StringBuilder();\n            int c;\n            do { c = read(); } while (c <= 32 && c != -1);\n            while (c > 32 && c != -1) {\n                sb.append((char) c);\n                c = read();\n            }\n            return sb.length() == 0 ? null : sb.toString();\n        }\n\n        private int read() throws IOException {\n            if (ptr >= len) {\n                len = in.read(buffer);\n                ptr = 0;\n                if (len <= 0) return -1;\n            }\n            return buffer[ptr++];\n        }\n    }\n}\n';
  }
}
