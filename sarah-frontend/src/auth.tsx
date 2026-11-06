import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, type AuthReply, type Me } from './api';
import { preview } from './preview';

interface AuthState {
  me: Me | null;
  loading: boolean;
  /** Re-reads the server session (call after a successful verification). */
  refresh: () => Promise<void>;
  /** Server-side logout — invalidates the session cookie. */
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState>(null as unknown as AuthState);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    if (preview) return;
    try {
      // Reads the server session and primes the XSRF-TOKEN cookie for later POSTs.
      const r = await api.get<AuthReply>('/api/auth/session');
      setMe(r.data.authenticated && r.data.email ? { email: r.data.email, roles: ['OWNER'] } : null);
    } catch {
      setMe(null);
    }
  }, []);

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, [refresh]);

  const logout = useCallback(async () => {
    try {
      await api.post('/api/auth/logout');
    } catch {
      /* best effort; clear locally regardless */
    }
    setMe(null);
  }, []);

  return (
    <AuthContext.Provider value={{ me, loading, refresh, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
