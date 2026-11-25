import { useEffect, useState, type FormEvent } from 'react';
import axios from 'axios';
import { api } from '../api';
import { useAuth } from '../auth';
import { Card, ErrorNotice } from '../components/Layout';
import { preview } from '../preview';

type Repo = { url: string; connected: boolean };
type Result = { url: string; model: string; changedFiles: number; alreadyExists: boolean };

export function SeoFixPanel({ auditId, kind }: { auditId: number | null; kind: string | null }) {
  const { activeWorkspaceId } = useAuth();
  const [repo, setRepo] = useState<Repo>({ url: '', connected: false });
  const [url, setUrl] = useState('');
  const [token, setToken] = useState('');
  const [saving, setSaving] = useState(false);
  const [fixing, setFixing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<Result | null>(null);

  useEffect(() => {
    setResult(null); setToken('');
    if (preview || !activeWorkspaceId) return;
    api.get<Repo>('/api/v1/seo/fixes/repository')
      .then(r => { setRepo(r.data); setUrl(r.data.url); })
      .catch(() => setError('Could not load GitHub connection for this company.'));
  }, [activeWorkspaceId]);

  const errorText = (e: unknown, fallback: string) => {
    if (axios.isAxiosError(e)) {
      const reason = e.response?.data?.detail ?? e.response?.data?.message;
      if (typeof reason === 'string' && reason.length < 500) return reason;
    }
    return fallback;
  };

  async function connect(e: FormEvent) {
    e.preventDefault();
    if (!token.trim() || !url.trim()) return;
    setSaving(true); setError(null); setResult(null);
    try {
      const response = await api.put<Repo>('/api/v1/seo/fixes/repository', {
        url: url.trim(), token: token.trim(),
      });
      setRepo(response.data); setUrl(response.data.url); setToken('');
    } catch (e) { setError(errorText(e, 'Cannot connect GitHub. Check the repository URL and token permissions.')); }
    finally { setSaving(false); }
  }

  async function disconnect() {
    if (!window.confirm('Remove this company’s GitHub connection? Existing PRs will remain on GitHub.')) return;
    setSaving(true); setError(null);
    try {
      await api.delete('/api/v1/seo/fixes/repository');
      setRepo({ url: '', connected: false }); setUrl(''); setToken(''); setResult(null);
    } catch (e) { setError(errorText(e, 'Cannot disconnect the repository.')); }
    finally { setSaving(false); }
  }

  async function generate() {
    if (!auditId || !repo.connected) return;
    if (!window.confirm('Use Astra to analyze the selected saved report and selected website source files? This uses paid API tokens and creates a DRAFT GitHub PR for your manual review. SARAH will NOT merge or deploy.')) return;
    setFixing(true); setError(null); setResult(null);
    try {
      const response = await api.post<Result>('/api/v1/seo/fixes/pull-request',
        { auditId, confirmed: true }, { timeout: 240000 });
      setResult(response.data);
    } catch (e) { setError(errorText(e, 'Could not create a safe fix PR. Review backend logs or try another audit.')); }
    finally { setFixing(false); }
  }

  const isSarahAgentRepo = /\/Sarah-AI-Agent-for-Communication-and-Marketing-\/?$/i.test(repo.url);
  return <Card title="Fix website issues with Astra → GitHub PR">
    <p>Connect your company's website repository. SARAH reads a small selection of website source files, proposes changes based on your audit, and creates a <strong>draft</strong> pull request. You review, merge and redeploy yourself.</p>
    <p className="muted">Use a fine-grained GitHub token restricted to this repository: <strong>Contents: read/write</strong> and <strong>Pull requests: read/write</strong>. The token is encrypted by SARAH and never shared with Astra.</p>
    {repo.connected && <p>Connected to <a href={repo.url} target="_blank" rel="noopener noreferrer">{repo.url}</a>.</p>}
    {isSarahAgentRepo && <p role="alert" style={{ fontWeight: 600 }}>
      This is SARAH's application repository, not necessarily the repository that deploys your audited website.
      To correct the SEO issues, connect the GitHub repository containing the website's actual HTML/templates.
      SARAH now checks that audit URLs map to files in the selected repository before calling Astra.
    </p>}
    <form onSubmit={connect} style={{ display: 'grid', gap: '0.7rem', maxWidth: 680, margin: '1rem 0' }}>
      <label>GitHub repository URL
        <input aria-label="Website GitHub repository URL" required type="url" value={url} onChange={e => setUrl(e.target.value)}
          placeholder="https://github.com/company/website" style={{ display: 'block', width: '100%', padding: '0.65rem' }}/>
      </label>
      <label>Fine-grained GitHub token {repo.connected ? '(to replace saved credentials)' : ''}
        <input aria-label="GitHub token" required type="password" autoComplete="off" value={token} onChange={e => setToken(e.target.value)}
          placeholder="github_pat_..." style={{ display: 'block', width: '100%', padding: '0.65rem' }}/>
      </label>
      <div style={{ display: 'flex', gap: '0.7rem', flexWrap: 'wrap' }}>
        <button className="button quiet" type="submit" disabled={preview || saving || !token.trim()}>{saving ? 'Connecting…' : repo.connected ? 'Update connection' : 'Connect repository'}</button>
        {repo.connected && <button type="button" className="button quiet" disabled={saving || fixing} onClick={() => void disconnect()}>Disconnect</button>}
      </div>
    </form>
    <p>{auditId ? <>Selected report: <strong>#{auditId}</strong> ({kind}).</> : <>Run a website audit or AI visibility measurement, or open a saved report above.</>}</p>
    <button className="button" disabled={preview || fixing || saving || !repo.connected || !auditId} onClick={() => void generate()}>
      {fixing ? 'Astra is preparing the code changes…' : 'Generate draft fix PR from this report'}
    </button>
    <p className="muted">SARAH will reject mismatched repositories before charging for AI-generated edits. Some findings (backlinks, third-party citations, Google indexing decisions) cannot be fixed by editing your repository. If no defensible edit exists, SARAH will not open a PR.</p>
    <ErrorNotice message={error}/>
    {result && <p role="status"><strong>{result.alreadyExists ? 'Existing proposal:' : 'Draft PR created:'}</strong>{' '}
      <a href={result.url} target="_blank" rel="noopener noreferrer">Review on GitHub</a>
      {' '} · {result.changedFiles} file(s) · {result.model}. No merge or deployment performed.
    </p>}
  </Card>;
}
