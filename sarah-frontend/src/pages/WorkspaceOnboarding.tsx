import { useState, type FormEvent } from 'react';
import axios from 'axios';
import { useAuth } from '../auth';
import { SarahMark } from '../components/Layout';

export function WorkspaceOnboarding() {
  const { me, workspaces, selectWorkspace, createWorkspace } = useAuth();
  const [name, setName] = useState('');
  const [website, setWebsite] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy || name.trim().length < 2) return;
    setBusy(true); setError(null);
    try { await createWorkspace(name.trim(), website.trim()); }
    catch (e) {
      const message = axios.isAxiosError(e) ? e.response?.data?.detail ?? e.response?.data?.message : null;
      setError(typeof message === 'string' ? message : 'Could not create your company workspace.');
    } finally { setBusy(false); }
  }

  return <div className="gate" style={{ minHeight: 'min(100vh, 1000px)' }}>
    <div className="onboard" style={{ maxWidth: 580, width: '100%' }}>
      <div className="brand"><SarahMark /> SARAH <span className="brand-caption">BY OREO STUDIOS</span></div>
      <div className="section" style={{ padding: '2rem', marginTop: '1rem' }}>
        <p className="eyebrow">Your company, your SARAH</p>
        <h1>Create a company workspace</h1>
        <p className="muted">Welcome {me?.email}. You will own this company's workspace, marketing, connected channels, and team. Other companies cannot access it.</p>
        {workspaces.length > 0 && <>
          <h2 style={{ marginTop: '1.5rem' }}>Existing workspaces</h2>
          {workspaces.map(w => <button type="button" key={w.id} className="button quiet" onClick={() => void selectWorkspace(w.id)}
            style={{ margin: '0.4rem' }}>{w.name} · {w.role}</button>)}
        </>}
        <form onSubmit={submit} style={{ display: 'grid', gap: '0.8rem', marginTop: '1.5rem' }}>
          <label>Company name
            <input required minLength={2} maxLength={160} value={name}
              onChange={e => setName(e.target.value)} placeholder="e.g. Oreo Studios"
              style={{ width: '100%', padding: '0.8rem', marginTop: '0.3rem' }}/>
          </label>
          <label>Company website (optional)
            <input type="url" value={website} onChange={e => setWebsite(e.target.value)}
              placeholder="https://company.com"
              style={{ width: '100%', padding: '0.8rem', marginTop: '0.3rem' }}/>
          </label>
          {error && <p className="error-notice" role="alert">{error}</p>}
          <button type="submit" className="button" disabled={busy || name.trim().length < 2}>
            {busy ? 'Creating…' : 'Create my company workspace'}
          </button>
        </form>
      </div>
    </div>
  </div>;
}
