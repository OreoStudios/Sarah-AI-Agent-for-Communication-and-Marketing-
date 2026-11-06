import { useEffect, useState } from 'react';
import { api, type Publication } from '../api';
import { Card, Empty, PageHeading, Status } from '../components/Layout';

export function Calendar() {
  const [upcoming, setUpcoming] = useState<Publication[]>([]);
  const [failed, setFailed] = useState<Publication[]>([]);
  const [published, setPublished] = useState<Publication[]>([]);

  function reload() {
    const now = new Date();
    const to = new Date(now.getTime() + 30 * 864e5);
    api.get<Publication[]>('/api/v1/publications/calendar', { params: { from: now.toISOString(), to: to.toISOString() } })
      .then((r) => setUpcoming(r.data));
    api.get<Publication[]>('/api/v1/publications/history', { params: { status: 'FAILED' } }).then((r) => setFailed(r.data));
    api.get<Publication[]>('/api/v1/publications/history', { params: { status: 'PUBLISHED' } }).then((r) => setPublished(r.data));
  }
  useEffect(reload, []);

  async function retry(id: string) { await api.post(`/api/v1/publications/${id}/retry`); reload(); }
  async function cancel(id: string) { await api.post(`/api/v1/publications/${id}/cancel`); reload(); }

  return (
    <>
      <PageHeading eyebrow="Content calendar" title="What goes out, and when" description="Scheduled, published, and anything that needs a second look." />

      <div className="stack">
        <Card title={`Scheduled · ${upcoming.length}`}>
          <PubList items={upcoming} onCancel={cancel} emptyText="Nothing queued. Approved content lands here." />
        </Card>
        <Card title={`Needs attention · ${failed.length}`}>
          <PubList items={failed} onRetry={retry} emptyText="No failures — a clean run." />
        </Card>
        <Card title={`Published · ${published.length}`}>
          <PubList items={published} emptyText="Nothing published yet." />
        </Card>
      </div>
    </>
  );
}

function PubList({ items, onRetry, onCancel, emptyText }: {
  items: Publication[]; onRetry?: (id: string) => void; onCancel?: (id: string) => void; emptyText: string;
}) {
  if (items.length === 0) return <Empty title="Empty">{emptyText}</Empty>;
  return (
    <div className="list">
      {items.map((p) => (
        <div className="record" key={p.id}>
          <div className="lead">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
              <span className="mono" style={{ textTransform: 'uppercase', fontSize: '0.66rem', letterSpacing: '0.1em', color: 'var(--accent)' }}>{p.platform}</span>
              <Status value={p.status} />
              <span className="mono" style={{ fontSize: '0.64rem', color: 'var(--ink-faint)' }}>{new Date(p.scheduledAt).toLocaleString()}</span>
            </div>
            <div className="sub" style={{ marginTop: '0.3rem' }}>{p.content}</div>
            {p.externalUrl && <a className="eyebrow" style={{ color: 'var(--accent)' }} href={p.externalUrl} target="_blank" rel="noreferrer">View post ↗</a>}
            {p.lastError && <div className="mono" style={{ fontSize: '0.68rem', color: 'var(--neg)', marginTop: '0.2rem' }}>{p.lastError}</div>}
          </div>
          <div style={{ display: 'flex', gap: '0.4rem', flex: 'none' }}>
            {onRetry && <button className="button small" onClick={() => onRetry(p.id)}>Retry</button>}
            {onCancel && <button className="button quiet small" onClick={() => onCancel(p.id)}>Cancel</button>}
          </div>
        </div>
      ))}
    </div>
  );
}
