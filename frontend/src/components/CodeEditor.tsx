import Editor from '@monaco-editor/react';
import type { SupportedUiLanguage } from '../types/contracts';

interface CodeEditorProps {
  value: string;
  language: SupportedUiLanguage;
  readOnly?: boolean;
  height?: string;
  onChange?: (value: string) => void;
}

export function CodeEditor({ value, language, readOnly = false, height = '420px', onChange }: CodeEditorProps) {
  return (
    <div className="code-editor">
      <Editor
        height={height}
        language={toMonacoLanguage(language)}
        theme="vs"
        value={value}
        onChange={(nextValue) => onChange?.(nextValue ?? '')}
        options={{
          readOnly,
          minimap: { enabled: false },
          fontFamily: 'Consolas, Monaco, Menlo, monospace',
          fontSize: 13,
          tabSize: 4,
          scrollBeyondLastLine: false,
          automaticLayout: true,
          lineNumbersMinChars: 3,
          wordWrap: 'on'
        }}
      />
    </div>
  );
}

function toMonacoLanguage(language: SupportedUiLanguage): string {
  switch (language) {
    case 'CPP':
      return 'cpp';
    case 'PYTHON':
      return 'python';
    case 'RUST':
      return 'rust';
    case 'JAVASCRIPT':
      return 'javascript';
    case 'JAVA':
    default:
      return 'java';
  }
}
