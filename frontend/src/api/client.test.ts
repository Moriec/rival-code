import { describe, expect, it } from 'vitest';
import { buildUrl } from './client';

describe('buildUrl', () => {
  it('serializes repeated query params for Spring list binding', () => {
    const url = new URL(buildUrl('/api/problems', { difficulties: ['EASY', 'HARD'], search: 'sum', page: 0 }));

    expect(url.pathname).toBe('/api/problems');
    expect(url.searchParams.getAll('difficulties')).toEqual(['EASY', 'HARD']);
    expect(url.searchParams.get('search')).toBe('sum');
    expect(url.searchParams.get('page')).toBe('0');
  });
});
