import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FormattedStatement } from './FormattedStatement';

describe('FormattedStatement', () => {
  it('renders Codeforces-style math, sections and lists', () => {
    const text = `You are given $$$m_1$$$ edges and $$$2\\cdot 10^5$$$ vertices.

- Add an edge between $$$u$$$ and $$$v$$$.
- Remove an edge.

Input:
The first line contains $$$t$$$.

1. First operation.
2. Second operation.`;

    const { container } = render(<FormattedStatement text={text} />);

    expect(container.querySelectorAll('.math-inline').length).toBeGreaterThanOrEqual(4);
    expect(screen.getByRole('heading', { name: 'Input' })).toBeInTheDocument();
    expect(screen.getAllByRole('list')).toHaveLength(2);
    expect(screen.getAllByRole('listitem')).toHaveLength(4);
  });

  it('renders common wrapped latex commands without leaking command names', () => {
    const { container } = render(<FormattedStatement text="Value $$$\\mathtt{0}$$$ and note $$$^{\\text{*}}$$$." />);

    expect(container.textContent).toContain('0');
    expect(container.textContent).toContain('*');
    expect(container.textContent).not.toContain('mathtt');
    expect(container.textContent).not.toContain('text');
  });
});
