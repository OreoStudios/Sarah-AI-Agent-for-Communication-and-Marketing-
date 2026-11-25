import { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { api } from '../api';
import { useAuth } from '../auth';
import { Card, PageHeading, ErrorNotice } from '../components/Layout';
import { SeoAnswer } from '../components/SeoAnswer';
import { SeoFixPanel } from '../components/SeoFixPanel';
import { preview } from '../preview';

type Job = { id: string; kind: string; status: 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';
  progress: { phase: string; discovered: number; analyzed: number; issues: number };
  reportId: number | null; error: string | null };
const sleep = (ms: number) => new Promise<void>(resolve => window.setTimeout(resolve, ms));
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

  const [job, setJob] = useState<Job | null>(null);
  const [historyWarning, setHistoryWarning] = useState<string | null>(null);
  const generation = useRef(0);

  function errorText(e: unknown, kind: string) {
    if (axios.isAxiosError(e)) {
      const status = e.response?.status;
      const detail = e.response?.data?.detail ?? e.response?.data?.message;
      if (typeof detail === 'string' && detail.length < 500 && !detail.startsWith('<')) return detail;
      if (status === 401 || status === 403) return 'Session or company access denied. Sign in and select the correct company.';
      if (status === 404) return 'Report/job not found. Check report history or whether SARAH was redeployed.';
      if (status === 409) return 'An SEO operation is already active, or the company website is not configured.';
      if (status === 429) return 'Rate limit exceeded. Try later.';
      if (status === 502 || status === 504 || e.code === 'ECONNABORTED' || e.code === 'ETIMEDOUT')
        return 'The server or proxy timed out. Check report history before starting another run.';
      if (!e.response) return 'Connection interrupted. Check report history before starting another run.';
    }
    return kind === 'crawl' ? 'Website audit failed. Check the configured website and server logs.' :
      'Report failed. Check provider settings and server logs.';
  }

  async function refresh(epoch = generation.current): Promise<History[]> {
    if (preview) return [];
    const response = await api.get<History[]>('/api/v1/seo/history');
    if (generation.current === epoch) setHistory(response.data);
    return response.data;
  }
  useEffect(() => {
    const epoch = ++generation.current;
    setReport(null); setSelectedId(null); setSelectedKind(null); setHistory([]);
    setBusy(false); setJob(null); setError(null); setHistoryWarning(null);
    void refresh(epoch).catch(() => { if (generation.current === epoch) setHistoryWarning('Unable to load saved SEO reports. Owner access required.'); });
    return () => { generation.current++; };
  }, [activeWorkspaceId]);

  async function run(kind: string) {
    if (kind === 'indexnow' && !window.confirm('Confirm this changed URL should be submitted to IndexNow?')) return;
    const epoch = generation.current;
    const current = () => generation.current === epoch;
    setBusy(true); setError(null); setHistoryWarning(null); setJob(null);
    setReport(null); setSelectedId(null); setSelectedKind(null);
    const payload = { target, url, keyword, query, provider, strategy, confirmSubmission: kind === 'indexnow' };
    try {
      if (kind === 'crawl' || kind === 'geo') {
        let next = (await api.post<Job>('/api/v1/seo/jobs/' + kind, payload, { timeout: 15000 })).data;
        let failures = 0;
        const deadline = Date.now() + 15 * 60 * 1000;
        while (current()) {
          setJob(next);
          if (next.status === 'FAILED') {
            setError(next.error ?? 'SEO job failed.');
            break;
          }
          if (next.status === 'SUCCEEDED') {
            if (next.reportId == null) throw new Error('Missing report ID');
            const saved = await api.get<Report>('/api/v1/seo/history/' + next.reportId, { timeout: 15000 });
            if (!current()) return;
            setReport(saved.data); setSelectedId(next.reportId); setSelectedKind(kind);
            break;
          }
          if (Date.now() > deadline) {
            setError('Still waiting for the SEO job. Check saved report history before retrying.');
            break;
          }
          await sleep(1500);
          if (!current()) return;
          try {
            next = (await api.get<Job>('/api/v1/seo/jobs/' + next.id, { timeout: 10000 })).data;
            failures = 0;
          } catch (e) {
            if (axios.isAxiosError(e) && [401, 403, 404].includes(e.response?.status ?? 0)) throw e;
            if (++failures >= 5) throw e;
          }
        }
      } else {
        const response = kind === 'inspect'
          ? await api.post<Report>('/api/v1/dashboard/search-console/inspect', { url })
          : await api.post<Report>('/api/v1/seo/' + kind, payload, { timeout: 140000 });
        if (!current()) return;
        setReport(response.data);
        // Only a saved report selected by its exact ID can be used for remediation.
      }
    } catch (e) {
      if (current()) setError(errorText(e, kind));
    } finally {
      if (current()) {
        setBusy(false);
        void refresh(epoch).catch(() => { if (current()) setHistoryWarning('Could not refresh history. Any displayed report is still available.'); });
      }
    }
  }

  async function load(item: History) {
    const epoch = generation.current;
    setError(null); setBusy(true); setJob(null);
    setReport(null); setSelectedId(null); setSelectedKind(null);
    try {
      const saved = (await api.get<Report>('/api/v1/seo/history/' + item.id)).data;
      if (generation.current !== epoch) return;
      setReport(saved); setSelectedId(item.id); setSelectedKind(item.kind);
    } catch (e) { if (generation.current === epoch) setError(errorText(e, item.kind)); }
    finally { if (generation.current === epoch) setBusy(false); }
  }

  return <>
    <PageHeading eyebrow="Search intelligence" title="SEO & GEO"
      description="Audit your company website, understand visibility gaps, and prepare human-reviewed code fixes. External API reports may incur charges."/>
    <ErrorNotice message={error}/>
    <ErrorNotice message={historyWarning}/>
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

    {busy && <Card title="SEO report progress">
      <div role="status" aria-live="polite">
        <p><strong>{job?.progress.phase ?? 'Starting report…'}</strong></p>
        {job?.kind === 'crawl' && <p>{job.progress.discovered} discovered · {job.progress.analyzed} analyzed · {job.progress.issues} findings so far</p>}
        <p className="muted">You can wait here for the result. Saved reports are also available in history.</p>
      </div>
    </Card>}
    {report && <Card title="Your report">
      {report.results ? <>
        <p><strong>{report.results.length} pages audited</strong> · {report.complete ? 'Discovered queue completed' : 'Partial crawl'}</p>
        {report.notes?.map((note, i) => <p className="muted" key={i}>{note}</p>)}
        <div style={{ overflowX: 'auto' }}>
          <table><thead><tr><th>URL</th><th>Status</th><th>Title</th><th>Issues found</th></tr></thead>
            <tbody>{report.results.map((p, i) => <tr key={i}><td>{p.url}</td><td>{p.status}</td><td>{p.title}</td><td>{p.issues.join(', ') || 'None detected'}</td></tr>)}</tbody>
          </table>
        </div>
        <h3>Prioritized findings</h3>
        {report.results.length === 0 ? <p>No pages were analyzed. Review the crawl notes before attempting remediation.</p> : <ul>
          {Object.entries(report.results.flatMap(p => p.issues).reduce<Record<string, number>>((counts, issue) => {
            const key = issue.split(':')[0]; counts[key] = (counts[key] ?? 0) + 1; return counts;
          }, {})).sort(([a], [b]) => Number(/^(HTTP_|BROKEN_LINK|FETCH_FAILED|NOINDEX|ROBOTS_BLOCKED)/.test(b)) - Number(/^(HTTP_|BROKEN_LINK|FETCH_FAILED|NOINDEX|ROBOTS_BLOCKED)/.test(a))).map(([issue, count]) =>
            <li key={issue}><strong>{/^(HTTP_|BROKEN_LINK|FETCH_FAILED|NOINDEX|ROBOTS_BLOCKED)/.test(issue) ? 'HIGH' : 'REVIEW'} · {issue}</strong> — {count} finding(s)</li>)}
        </ul>}
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
      <button className="button quiet" disabled={busy || preview} onClick={() => void refresh().then(() => setHistoryWarning(null)).catch(() => setHistoryWarning("Could not refresh saved reports."))}>Refresh saved reports</button>
      {history.map(item => <p key={item.id}>
        <button className="button quiet small" disabled={busy} onClick={() => void load(item)}>
          {item.kind} · {item.subject} · {new Date(item.at).toLocaleString()}
        </button>
      </p>)}
    </Card>
  </>;
}
