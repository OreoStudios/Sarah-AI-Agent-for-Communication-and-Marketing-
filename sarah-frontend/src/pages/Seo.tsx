import { useEffect, useState } from 'react';
import axios from 'axios';
import { api } from '../api';
import { useAuth } from '../auth';
import { Card, PageHeading, ErrorNotice } from '../components/Layout';
import { SeoAnswer } from '../components/SeoAnswer';
import { SeoFixPanel } from '../components/SeoFixPanel';
import { preview } from '../preview';

type History = { id: number; kind: string; subject: string; at: string };
type Page = { url: string; status: number; title: string; description: string; issues: string[] };
type Recommendation = { priority: string; title: string; explanation: string; fixableInRepository: boolean };
type PlanAction = { issue: string; affectedPagesOrLinks: number; priority: string; sampleUrls: string[] };
type Report = {
  results?: Page[]; notes?: string[]; complete?: boolean; actions?: PlanAction[];
  answer?: string; citationUrls?: string[]; brandMention?: boolean; ownDomainCited?: boolean;
  model?: string; provider?: string; status?: string; query?: string;
  recommendedActions?: Recommendation[]; measurementNote?: string;
};

function geoRecommendations(report: Report): Recommendation[] {
  if (report.recommendedActions?.length) return report.recommendedActions;
  const result: Recommendation[] = [];
  if (!report.brandMention) result.push({
    priority: 'HIGH', title: 'Explain your services and expertise clearly',
    explanation: 'Improve relevant service pages, page titles and accurate company context that answers the search question. This cannot guarantee a future AI mention.',
    fixableInRepository: true,
  });
  if (!report.ownDomainCited) result.push({
    priority: 'HIGH', title: 'Make your business pages discoverable',
    explanation: 'Review indexability, metadata, canonical links, internal linking and structured data where appropriate.',
    fixableInRepository: true,
  });
  result.push({
    priority: 'MEDIUM', title: 'Build genuine independent references',
    explanation: 'Earn real third-party citations and links, and test multiple neutral questions. A GitHub code change cannot create genuine external endorsements.',
    fixableInRepository: false,
  });
  return result;
}

export function Seo() {
  const { activeWorkspaceId } = useAuth();
  const [target, setTarget] = useState('oreostudios.fr');
  const [url, setUrl] = useState('https://oreostudios.fr/');
  const [keyword, setKeyword] = useState('création site web Strasbourg');
  const [query, setQuery] = useState('Quelle agence web choisir à Strasbourg pour créer un site professionnel ?');
  const [provider, setProvider] = useState('openai');
  const [strategy, setStrategy] = useState('mobile');
  const [report, setReport] = useState<Report | null>(null);
  const [history, setHistory] = useState<History[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedKind, setSelectedKind] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function refresh(): Promise<History[]> {
    if (preview) return [];
    const response = await api.get<History[]>('/api/v1/seo/history');
    setHistory(response.data);
    return response.data;
  }
  useEffect(() => {
    setReport(null); setSelectedId(null); setSelectedKind(null);
    void refresh().catch(() => setError('Unable to load this company’s SEO reports. Owner access required.'));
  }, [activeWorkspaceId]);

  async function run(kind: string) {
    if (kind === 'indexnow' && !window.confirm('Confirm this changed URL should be submitted to IndexNow?')) return;
    setBusy(true); setError(null);
    try {
      const response = kind === 'inspect'
        ? await api.post<Report>('/api/v1/dashboard/search-console/inspect', { url })
        : await api.post<Report>('/api/v1/seo/' + kind,
            { target, url, keyword, query, provider, strategy, confirmSubmission: kind === 'indexnow' },
            { timeout: 140000 });
      setReport(response.data);
      const rows = await refresh();
      const match = rows.find(h => h.kind === kind && h.kind !== 'inspect');
      setSelectedId(match?.id ?? null);
      setSelectedKind(match?.kind ?? null);
    } catch (e) {
      const message = axios.isAxiosError(e) ? e.response?.data?.detail ?? e.response?.data?.message : null;
      setError(typeof message === 'string' && message.length < 500 ? message
        : 'Report unavailable. Verify provider credentials, quota and your company access.');
    } finally { setBusy(false); }
  }

  async function load(item: History) {
    setError(null); setBusy(true);
    try {
      setReport((await api.get<Report>('/api/v1/seo/history/' + item.id)).data);
      setSelectedId(item.id); setSelectedKind(item.kind);
    } catch { setError('Could not load this saved report.'); }
    finally { setBusy(false); }
  }

  return <>
    <PageHeading eyebrow="Search intelligence" title="SEO & GEO"
      description="Audit your company website, understand visibility gaps, and prepare human-reviewed code fixes. External API reports may incur charges."/>
    <ErrorNotice message={error}/>
    <Card title="Website audit">
      <p>Discover same-origin HTML pages through links and sitemaps, then identify SEO issues SARAH can help address in your source code. Robots rules and crawl limits apply.</p>
      <button className="button" disabled={busy || preview} onClick={() => void run('crawl')}>Run website audit</button>
      <p><label>Page URL <input value={url} onChange={e => setUrl(e.target.value)}/></label>{' '}
        <select value={strategy} onChange={e => setStrategy(e.target.value)}>
          <option value="mobile">mobile</option><option value="desktop">desktop</option>
        </select>{' '}
        <button className="button quiet" disabled={busy || preview} onClick={() => void run('speed')}>PageSpeed</button>
      </p>
      <button className="button quiet" disabled={busy || preview} onClick={() => void run('inspect')}>Google index inspection</button>{' '}
      <button className="button quiet" disabled={busy || preview} onClick={() => void run('bing')}>Bing traffic</button>{' '}
      <button className="button quiet" disabled={busy || preview} onClick={() => void run('indexnow')}>Submit changed URL (IndexNow)</button>
    </Card>

    <Card title="Backlinks & keywords">
      <p><label>Domain <input value={target} onChange={e => setTarget(e.target.value)}/></label></p>
      {['summary', 'backlinks', 'referring-domains', 'lost-links', 'backlink-trend', 'ranked'].map(k =>
        <button key={k} className="button quiet" disabled={busy || preview} onClick={() => void run(k)}>{k}</button>)}
      <p><label>Keyword <input value={keyword} onChange={e => setKeyword(e.target.value)}/></label>{' '}
        <button className="button quiet" disabled={busy || preview} onClick={() => void run('keywords')}>Keyword suggestions</button></p>
    </Card>

    <Card title="AI visibility">
      <p>Ask a neutral search question and see whether the response mentions your company or cites its website. These are individual AI responses, not authoritative search rankings.</p>
      <label>Query <textarea value={query} onChange={e => setQuery(e.target.value)}/></label>
      <select value={provider} onChange={e => setProvider(e.target.value)}>
        <option value="openai">OpenAI</option><option value="anthropic">Claude</option><option value="gemini">Gemini</option>
      </select>{' '}
      <button className="button" disabled={busy || preview} onClick={() => void run('geo')}>Measure visibility & get recommendations</button>
    </Card>

    {busy && <p role="status">Running report…</p>}
    {report && <Card title="Your report">
      {report.results ? <>
        <p><strong>{report.results.length} pages audited</strong> · {report.complete ? 'Discovered queue completed' : 'Partial crawl'}</p>
        {report.notes?.map((note, i) => <p className="muted" key={i}>{note}</p>)}
        <div style={{ overflowX: 'auto' }}>
          <table><thead><tr><th>URL</th><th>Status</th><th>Title</th><th>Issues found</th></tr></thead>
            <tbody>{report.results.map((p, i) => <tr key={i}><td>{p.url}</td><td>{p.status}</td><td>{p.title}</td><td>{p.issues.join(', ') || 'None detected'}</td></tr>)}</tbody>
          </table>
        </div>
        <p className="muted">Only supported findings that can be tied to the repository will be proposed in a draft PR.</p>
      </> : report.answer ? <>
        <p><strong>{report.model}</strong> · Company mentioned: <strong>{report.brandMention ? 'Yes' : 'No'}</strong> ·
          Company website cited: <strong>{report.ownDomainCited ? 'Yes' : 'No'}</strong></p>
        <h3>What SARAH recommends fixing</h3>
        <ul>{geoRecommendations(report).map((a, i) => <li key={i} style={{ marginBottom: '0.9rem' }}>
          <strong>{a.priority} · {a.title}</strong>
          <p>{a.explanation}</p>
          <small>{a.fixableInRepository ? 'Potentially addressable in a GitHub PR' : 'Needs external / manual action'}</small>
        </li>)}</ul>
        <h3>AI search answer (sample)</h3>
        <SeoAnswer answer={report.answer}/>
        {(report.citationUrls?.length ?? 0) > 0 && <>
          <h3>Sources cited by this sample</h3>
          <ul>{report.citationUrls?.map((source, i) => <li key={i}><a href={source} target="_blank" rel="noopener noreferrer">{source}</a></li>)}</ul>
        </>}
        <p className="muted">{report.measurementNote ?? 'One provider response is a sample and does not guarantee future citations or rankings.'}</p>
      </> : report.actions ? <>
        <p>SEO action plan based on your latest crawl.</p>
        <ul>{report.actions.map((item, i) => <li key={i}>
          <strong>{item.priority} · {item.issue}</strong> — {item.affectedPagesOrLinks} affected page(s)
          {item.sampleUrls?.[0] && <p className="muted">Example: {item.sampleUrls[0]}</p>}
        </li>)}</ul>
      </> : <pre style={{ overflow: 'auto', maxHeight: 480 }}>{JSON.stringify(report, null, 2)}</pre>}
    </Card>}

    <SeoFixPanel auditId={selectedId} kind={selectedKind}/>
    <Card title="Report history">
      {history.map(item => <p key={item.id}>
        <button className="button quiet small" disabled={busy} onClick={() => void load(item)}>
          {item.kind} · {item.subject} · {new Date(item.at).toLocaleString()}
        </button>
      </p>)}
    </Card>
  </>;
}
