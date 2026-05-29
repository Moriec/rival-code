import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { StatusBadge } from './StatusBadge';

describe('StatusBadge', () => {
  it('uses success tone for accepted verdicts', () => {
    render(<StatusBadge status="ACCEPTED" />);

    expect(screen.getByText('ACCEPTED')).toHaveClass('badge--success');
  });

  it('uses danger tone for failed verdicts', () => {
    render(<StatusBadge status="WRONG_ANSWER" />);

    expect(screen.getByText('WRONG_ANSWER')).toHaveClass('badge--danger');
  });
});
