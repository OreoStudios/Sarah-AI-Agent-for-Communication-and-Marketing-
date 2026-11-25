import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, type AuthReply, type Me } from './api';
import { preview } from './preview';

export interface CompanyWorkspace { id: string; name: string; website: string | null; role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'; }
interface WorkspaceList { workspaces: CompanyWorkspace[]; activeWorkspaceId: string | null; }
interface AuthState {
  me: Me | null;
  loading: boolean;
  workspaces: CompanyWorkspace[];
  activeWorkspaceId: string | null;
  refresh: () => Promise<void>;
  createWorkspace: (name: string, website: string) => Promise<void>;
  selectWorkspace: (id: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthState>(null as unknown as AuthState);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null);
  const [workspaces, setWorkspaces] = useState<CompanyWorkspace[]>([]);
  const [activeWorkspaceId, setActiveWorkspaceId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    if (preview) return;
    try {
      // GET primes the XSRF cookie. All mutations preserve the HttpOnly session cookie.
      const session = await api.get<AuthReply>('/api/auth/session');
      if (!session.data.authenticated || !session.data.email) {
        setMe(null); setWorkspaces([]); setActiveWorkspaceId(null); return;
      }
      const r = await api.get<WorkspaceList>('/api/workspaces');
      const active = r.data.workspaces.find(w => w.id === r.data.activeWorkspaceId);
      setMe({ email: session.data.email, roles: active ? [active.role] : [] });
      setWorkspaces(r.data.workspaces);
      setActiveWorkspaceId(active?.id ?? null);
    } catch {
      setMe(null); setWorkspaces([]); setActiveWorkspaceId(null);
    }
  }, []);

  useEffect(() => { refresh().finally(() => setLoading(false)); }, [refresh]);

  const createWorkspace = useCallback(async (name: string, website: string) => {
    await api.post('/api/workspaces', { name, website: website || null });
    await refresh();
  }, [refresh]);

  const selectWorkspace = useCallback(async (id: string) => {
    await api.post(`/api/workspaces/${encodeURIComponent(id)}/select`);
    await refresh();
  }, [refresh]);

  const logout = useCallback(async () => {
    try { await api.post('/api/auth/logout'); } catch { /* local clear as fallback */ }
    setMe(null); setWorkspaces([]); setActiveWorkspaceId(null);
  }, []);

  return <AuthContext.Provider value={{
    me, loading, workspaces, activeWorkspaceId, refresh, createWorkspace, selectWorkspace, logout,
  }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
