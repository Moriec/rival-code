import type { ReactNode } from 'react';

interface FormattedStatementProps {
  text?: string | null;
  compact?: boolean;
  fallback?: string;
}

type StatementBlock =
  | { type: 'paragraph'; lines: string[] }
  | { type: 'section'; title: string }
  | { type: 'list'; ordered: boolean; items: string[] };

const LATEX_SYMBOLS: Record<string, string> = {
  alpha: 'α',
  beta: 'β',
  gamma: 'γ',
  delta: 'δ',
  epsilon: 'ε',
  theta: 'θ',
  lambda: 'λ',
  mu: 'μ',
  pi: 'π',
  sigma: 'σ',
  phi: 'φ',
  omega: 'ω',
  le: '≤',
  leq: '≤',
  ge: '≥',
  geq: '≥',
  neq: '≠',
  ne: '≠',
  cdot: '⋅',
  times: '×',
  div: '÷',
  pm: '±',
  mp: '∓',
  ldots: '…',
  dots: '…',
  to: '→',
  rightarrow: '→',
  leftarrow: '←',
  infty: '∞',
  in: '∈',
  notin: '∉',
  cup: '∪',
  cap: '∩',
  subset: '⊂',
  subseteq: '⊆',
  supset: '⊃',
  supseteq: '⊇',
  lfloor: '⌊',
  rfloor: '⌋',
  lceil: '⌈',
  rceil: '⌉',
  dagger: '†',
  ddagger: '‡',
  ',': ' '
};

const TEXT_COMMANDS = new Set(['text', 'mathrm', 'mathbf', 'mathtt', 'operatorname']);

export function FormattedStatement({ text, compact = false, fallback = 'Not specified' }: FormattedStatementProps) {
  const blocks = parseStatementBlocks(text ?? fallback);

  return (
    <div className={compact ? 'statement statement--compact' : 'statement'}>
      {blocks.map((block, index) => renderBlock(block, index))}
    </div>
  );
}

function renderBlock(block: StatementBlock, index: number) {
  if (block.type === 'section') {
    return (
      <h3 className="statement__section-title" key={`section-${index}`}>
        {block.title}
      </h3>
    );
  }

  if (block.type === 'list') {
    const Tag = block.ordered ? 'ol' : 'ul';
    return (
      <Tag className="statement__list" key={`list-${index}`}>
        {block.items.map((item, itemIndex) => (
          <li key={`${index}-${itemIndex}`}>{renderInlineContent(item)}</li>
        ))}
      </Tag>
    );
  }

  return (
    <p className="statement__paragraph" key={`paragraph-${index}`}>
      {renderInlineContent(block.lines.join(' '))}
    </p>
  );
}

function parseStatementBlocks(value: string): StatementBlock[] {
  const lines = value.replace(/\r\n?/g, '\n').split('\n');
  const blocks: StatementBlock[] = [];
  let paragraph: string[] = [];

  function flushParagraph() {
    const clean = paragraph.map((line) => line.trim()).filter(Boolean);
    if (clean.length > 0) {
      blocks.push({ type: 'paragraph', lines: clean });
    }
    paragraph = [];
  }

  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index].trim();
    if (!line) {
      flushParagraph();
      continue;
    }

    const sectionTitle = parseSectionTitle(line);
    if (sectionTitle) {
      flushParagraph();
      blocks.push({ type: 'section', title: sectionTitle });
      continue;
    }

    const bullet = parseBullet(line);
    if (bullet) {
      flushParagraph();
      const items = [bullet.text];
      while (index + 1 < lines.length) {
        const next = parseBullet(lines[index + 1].trim());
        if (!next || next.ordered !== bullet.ordered) {
          break;
        }
        items.push(next.text);
        index += 1;
      }
      blocks.push({ type: 'list', ordered: bullet.ordered, items });
      continue;
    }

    paragraph.push(line);
  }

  flushParagraph();
  return blocks.length > 0 ? blocks : [{ type: 'paragraph', lines: [value] }];
}

function parseSectionTitle(line: string): string | null {
  if (!/^[A-Z][A-Za-z ]{1,32}:$/.test(line)) {
    return null;
  }
  return line.slice(0, -1);
}

function parseBullet(line: string): { ordered: boolean; text: string } | null {
  const unordered = line.match(/^[-*]\s+(.+)$/);
  if (unordered) {
    return { ordered: false, text: unordered[1] };
  }

  const ordered = line.match(/^\d+[.)]\s+(.+)$/);
  if (ordered) {
    return { ordered: true, text: ordered[1] };
  }

  return null;
}

function renderInlineContent(value: string): ReactNode[] {
  const parts = value.split('$$$');
  return parts.map((part, index) => {
    if (index % 2 === 0) {
      return <span key={`text-${index}`}>{part}</span>;
    }
    return (
      <span className="math-inline" key={`math-${index}`}>
        {renderMathContent(part.trim(), `math-${index}`)}
      </span>
    );
  });
}

function renderMathContent(value: string, keyPrefix: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  let index = 0;
  let nodeIndex = 0;

  while (index < value.length) {
    const char = value[index];

    if (char === '^' || char === '_') {
      const script = readScript(value, index + 1);
      const Tag = char === '^' ? 'sup' : 'sub';
      nodes.push(
        <Tag key={`${keyPrefix}-script-${nodeIndex}`}>
          {renderMathContent(script.value, `${keyPrefix}-script-${nodeIndex}`)}
        </Tag>
      );
      index = script.nextIndex;
      nodeIndex += 1;
      continue;
    }

    if (char === '\\') {
      if (value[index + 1] === '\\') {
        index += 1;
        continue;
      }

      const command = readCommand(value, index);
      if (TEXT_COMMANDS.has(command.name) && value[command.nextIndex] === '{') {
        const group = readBalancedGroup(value, command.nextIndex);
        nodes.push(
          <span className={command.name === 'mathtt' ? 'math-monospace' : 'math-text'} key={`${keyPrefix}-cmd-${nodeIndex}`}>
            {renderMathContent(group.value, `${keyPrefix}-cmd-${nodeIndex}`)}
          </span>
        );
        index = group.nextIndex;
        nodeIndex += 1;
        continue;
      }

      if (command.name === 'not' && value[command.nextIndex] === '=') {
        nodes.push(<span key={`${keyPrefix}-cmd-${nodeIndex}`}>≠</span>);
        index = command.nextIndex + 1;
        nodeIndex += 1;
        continue;
      }

      nodes.push(<span key={`${keyPrefix}-cmd-${nodeIndex}`}>{LATEX_SYMBOLS[command.name] ?? command.name}</span>);
      index = command.nextIndex;
      nodeIndex += 1;
      continue;
    }

    if (char === '{') {
      const group = readBalancedGroup(value, index);
      nodes.push(
        <span key={`${keyPrefix}-group-${nodeIndex}`}>
          {renderMathContent(group.value, `${keyPrefix}-group-${nodeIndex}`)}
        </span>
      );
      index = group.nextIndex;
      nodeIndex += 1;
      continue;
    }

    if (char === '}') {
      index += 1;
      continue;
    }

    nodes.push(<span key={`${keyPrefix}-char-${nodeIndex}`}>{char}</span>);
    index += 1;
    nodeIndex += 1;
  }

  return nodes;
}

function readScript(value: string, startIndex: number): { value: string; nextIndex: number } {
  let index = startIndex;
  while (value[index] === ' ') {
    index += 1;
  }

  if (value[index] === '{') {
    return readBalancedGroup(value, index);
  }

  if (value[index] === '\\') {
    const command = readCommand(value, index);
    return { value: LATEX_SYMBOLS[command.name] ?? command.name, nextIndex: command.nextIndex };
  }

  return { value: value[index] ?? '', nextIndex: Math.min(index + 1, value.length) };
}

function readBalancedGroup(value: string, startIndex: number): { value: string; nextIndex: number } {
  let depth = 0;
  let index = startIndex;

  for (; index < value.length; index += 1) {
    if (value[index] === '{') {
      depth += 1;
      continue;
    }
    if (value[index] === '}') {
      depth -= 1;
      if (depth === 0) {
        return { value: value.slice(startIndex + 1, index), nextIndex: index + 1 };
      }
    }
  }

  return { value: value.slice(startIndex + 1), nextIndex: value.length };
}

function readCommand(value: string, startIndex: number): { name: string; nextIndex: number } {
  let index = startIndex + 1;
  while (/[A-Za-z]/.test(value[index] ?? '')) {
    index += 1;
  }

  if (index === startIndex + 1) {
    return { name: value[index] ?? '', nextIndex: Math.min(index + 1, value.length) };
  }

  return { name: value.slice(startIndex + 1, index), nextIndex: index };
}
