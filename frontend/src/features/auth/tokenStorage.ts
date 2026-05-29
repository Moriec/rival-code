import type { AuthTokensResponse, UserProfileDto } from '../../types/contracts';

const STORAGE_KEY = 'rivalcode.auth';

export interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresInSeconds?: number;
  user?: UserProfileDto;
}

export function getStoredAuth(): StoredAuth | null {
  const raw = window.sessionStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return migrateLegacyLocalStorageAuth();
  }
  try {
    return JSON.parse(raw) as StoredAuth;
  } catch {
    window.sessionStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

export function setStoredAuth(auth: AuthTokensResponse | StoredAuth): StoredAuth {
  const stored: StoredAuth = {
    accessToken: auth.accessToken,
    refreshToken: auth.refreshToken,
    tokenType: auth.tokenType ?? 'Bearer',
    expiresInSeconds: auth.expiresInSeconds,
    user: auth.user
  };
  window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  window.localStorage.removeItem(STORAGE_KEY);
  window.dispatchEvent(new Event('rivalcode:auth-changed'));
  return stored;
}

export function updateStoredUser(user: UserProfileDto): void {
  const current = getStoredAuth();
  if (!current) {
    return;
  }
  setStoredAuth({ ...current, user });
}

export function clearStoredAuth(): void {
  window.sessionStorage.removeItem(STORAGE_KEY);
  window.localStorage.removeItem(STORAGE_KEY);
  window.dispatchEvent(new Event('rivalcode:auth-changed'));
}

export function getAccessToken(): string | null {
  return getStoredAuth()?.accessToken ?? null;
}

export function getRefreshToken(): string | null {
  return getStoredAuth()?.refreshToken ?? null;
}

function migrateLegacyLocalStorageAuth(): StoredAuth | null {
  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const auth = JSON.parse(raw) as StoredAuth;
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    window.localStorage.removeItem(STORAGE_KEY);
    return auth;
  } catch {
    window.localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}
