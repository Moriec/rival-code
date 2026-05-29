import { beforeEach, describe, expect, it } from 'vitest';
import { clearStoredAuth, getAccessToken, getRefreshToken, getStoredAuth, setStoredAuth } from './tokenStorage';

describe('tokenStorage', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    window.localStorage.clear();
  });

  it('stores access and refresh tokens per tab', () => {
    setStoredAuth({
      accessToken: 'access',
      refreshToken: 'refresh',
      user: { userId: 'u1', username: 'tester' }
    });

    expect(getAccessToken()).toBe('access');
    expect(getRefreshToken()).toBe('refresh');
    expect(getStoredAuth()?.user?.username).toBe('tester');
    expect(window.localStorage.getItem('rivalcode.auth')).toBeNull();
  });

  it('clears tokens', () => {
    setStoredAuth({ accessToken: 'access', refreshToken: 'refresh' });
    clearStoredAuth();

    expect(getAccessToken()).toBeNull();
  });

  it('migrates legacy localStorage auth into sessionStorage', () => {
    window.localStorage.setItem(
      'rivalcode.auth',
      JSON.stringify({ accessToken: 'legacy-access', refreshToken: 'legacy-refresh' })
    );

    expect(getAccessToken()).toBe('legacy-access');
    expect(window.localStorage.getItem('rivalcode.auth')).toBeNull();
    expect(window.sessionStorage.getItem('rivalcode.auth')).toContain('legacy-access');
  });
});
