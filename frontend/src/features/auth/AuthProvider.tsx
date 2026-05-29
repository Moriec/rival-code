import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../../api/endpoints';
import type { AuthTokensResponse, LoginRequest, RegisterRequest, UpdateProfileRequest, UserProfileDto } from '../../types/contracts';
import { clearStoredAuth, getStoredAuth, setStoredAuth, updateStoredUser, type StoredAuth } from './tokenStorage';

interface AuthContextValue {
  auth: StoredAuth | null;
  user: UserProfileDto | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  updateProfile: (request: UpdateProfileRequest) => Promise<void>;
  refreshUser: () => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<StoredAuth | null>(() => getStoredAuth());
  const [isLoading, setIsLoading] = useState(Boolean(auth?.accessToken && !auth.user));

  const syncFromStorage = useCallback(() => {
    setAuth(getStoredAuth());
  }, []);

  useEffect(() => {
    window.addEventListener('rivalcode:auth-changed', syncFromStorage);
    window.addEventListener('rivalcode:auth-expired', syncFromStorage);
    return () => {
      window.removeEventListener('rivalcode:auth-changed', syncFromStorage);
      window.removeEventListener('rivalcode:auth-expired', syncFromStorage);
    };
  }, [syncFromStorage]);

  const applyTokens = useCallback((tokens: AuthTokensResponse) => {
    setAuth(setStoredAuth(tokens));
  }, []);

  const refreshUser = useCallback(async () => {
    if (!getStoredAuth()?.accessToken) {
      return;
    }
    setIsLoading(true);
    try {
      const user = await authApi.me();
      updateStoredUser(user);
      setAuth(getStoredAuth());
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (auth?.accessToken && !auth.user) {
      void refreshUser();
    }
  }, [auth?.accessToken, auth?.user, refreshUser]);

  const login = useCallback(
    async (request: LoginRequest) => {
      applyTokens(await authApi.login(request));
    },
    [applyTokens]
  );

  const register = useCallback(
    async (request: RegisterRequest) => {
      applyTokens(await authApi.register(request));
    },
    [applyTokens]
  );

  const updateProfile = useCallback(async (request: UpdateProfileRequest) => {
    const user = await authApi.updateMe(request);
    updateStoredUser(user);
    setAuth(getStoredAuth());
  }, []);

  const logout = useCallback(() => {
    clearStoredAuth();
    setAuth(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      auth,
      user: auth?.user ?? null,
      isAuthenticated: Boolean(auth?.accessToken),
      isLoading,
      login,
      register,
      updateProfile,
      refreshUser,
      logout
    }),
    [auth, isLoading, login, logout, refreshUser, register, updateProfile]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return value;
}
