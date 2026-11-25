import { useEffect, useState, type FormEvent } from 'react';
import axios from 'axios';
import { api } from '../api';
import { useAuth } from '../auth';
import { Card, PageHeading, Loading, ErrorNotice } from '../components/Layout';

interface Member { email: string; role: string; }
export function Team() {
  const { me, workspaces, activeWorkspaceId } = useAuth();
  const active = workspaces.find(w => w.id === activeWorkspaceId);
  const [members, setMembers] = useState<Member[]>([]);
  const [email, setEmail] = useState('');
  const [role, setRole] = useState('MEMBER');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    if (!active || active.role !== 'OWNER') { setLoading(false); return; }
    try {
      const result = await api.get<Member[]>(`/api/workspaces/${active.id}/members`);
      setMembers(result.data); setError(null);
    } catch { setError('Unable to load company members.'); }
    finally { setLoading(false); }
  }
  useEffect(() => { setLoading(true); void load(); }, [activeWorkspaceId]);

  async function invite(e: FormEvent) {
    e.preventDefault();
    if (!active || !email.trim() || busy) return;
    setBusy(true); setError(null);
    try {
      await api.post(`/api/workspaces/${active.id}/members`, { email: email.trim(), role });
      setEmail(''); await load();
    } catch (e) {
      const detail = axios.isAxiosError(e) ? e.response?.data?.detail : null;
      setError(typeof detail === 'string' ? detail : 'Could not add this member.');
    } finally { setBusy(false); }
  }

  async function revoke(member: Member) {
    if (!active || member.role === 'OWNER' || busy) return;
    if (!window.confirm(`Remove ${member.email} from ${active.name}?`)) return;
    setBusy(true);
    try { await api.delete(`/api/workspaces/${active.id}/members/${encodeURIComponent(member.email)}`); await load(); }
    catch { setError('Could not remove this member.'); }
    finally { setBusy(false); }
  }

  return <>
    <PageHeading eyebrow="Organization" title="Company team"
      description={`Manage access to ${active?.name ?? 'this company'}. Membership never grants access to other companies.`} />
    {!active || active.role !== 'OWNER' ? <Card title="Team access">
      Only the company owner can manage memberships.
    </Card> : <>
      <Card title="Add a member">
        <p className="muted">The invited employee must sign in with this exact email and complete verification. No invitation email is sent by this feature.</p>
        <form onSubmit={invite} style={{ display: 'flex', gap: '0.8rem', flexWrap: 'wrap', marginTop: '1rem' }}>
          <input type="email" required value={email} onChange={e => setEmail(e.target.value)}
            placeholder="employee@company.com" aria-label="Employee email" style={{ padding: '0.7rem', flex: '1 1 240px' }}/>
          <select value={role} onChange={e => setRole(e.target.value)} aria-label="Employee role" style={{ padding: '0.7rem' }}>
            <option value="ADMIN">Admin</option><option value="MEMBER">Member</option><option value="VIEWER">Viewer (read only)</option>
          </select>
          <button className="button" type="submit" disabled={busy}>{busy ? 'Saving…' : 'Add member'}</button>
        </form>
        <ErrorNotice message={error}/>
      </Card>
      <Card title="Members">
        {loading ? <Loading /> : members.map(m => <div className="record" key={m.email}>
          <div className="lead"><span className="title">{m.email}</span><span className="sub">{m.role}</span></div>
          {m.role !== 'OWNER' && m.email !== me?.email && <button className="button quiet small" onClick={() => void revoke(m)} disabled={busy}>Remove</button>}
        </div>)}
      </Card>
    </>}
  </>;
}
