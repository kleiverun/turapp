import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import type { UserResponse } from '../api/types';

const STORAGE_KEY = 'turapp.user';

interface AuthState {
  user: UserResponse | null;
  setUser: (user: UserResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

function loadStoredUser(): UserResponse | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as UserResponse) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<UserResponse | null>(loadStoredUser);

  const setUser = useCallback((u: UserResponse) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(u));
    setUserState(u);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setUserState(null);
  }, []);

  return <AuthContext.Provider value={{ user, setUser, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
