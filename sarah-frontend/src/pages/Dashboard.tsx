import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, type DashboardSummary } from '../api';
import { Badge, Card, Empty, ErrorNotice, Loading, PageHeading, Status, humanize } from '../components/Layout';

export function Dashboard() {
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load() {
    setError(null);
    api.get<DashboardSummary>('/api/v1/dashboard/summary')
      .then((r) => setData(r.data))
      .catch(() => setError('Could not reach the backend. Is it running on :8080?'));
  }
  useEffect(load, []);

  if (error) return <ErrorNotice message={error} retry={load} />;
  if (!data) return <Loading />;

  const total = Object.values(data.campaignCountsByStatus).reduce((a, b) => a + b, 0);
  const connected = data.channels.filter((c) => c.status === 'CONNECTED').length;

  // The living status line — one accent moment that says what Sarah is doing.
  const state = !data.autonomyEnabled
    ? { cls: 'paused', text: 'Holding — the kill switch is engaged.' }
    : data.upcomingPublications.length > 0
      ? { cls: 'live', text: `Scheduling ${data.upcomingPublications.length} publication${data.upcomingPublications.length > 1 ? 's' : ''}.` }
      : { cls: 'idle', text: 'Standing by for your next brief.' };

  return (
    <>
      <PageHeading
        eyebrow="Intelligence"
        title="Today, at a glance"
        description="What Sarah is working on, what's live, and where your attention is worth spending."
        actions={
          <span className="sarah-status">
            <span className={`nib ${state.cls}`} />
            {state.text}
          </span>
        }
      />

      <div className="stack">
        <div className="ledger">
          <Figure n={total} label="Campaigns" />
          <Figure n={data.activeCampaigns.length} label="In motion" />
          <Figure n={data.upcomingPublications.length} label="Upcoming posts" />
          <Figure n={connected} label={`Channels live / ${data.channels.length}`} />
        </div>

        <div className="grid grid-lead">
          <Card
            title="In motion"
            actions={<Link className="eyebrow" to="/campaigns" style={{ color: 'var(--accent)' }}>All campaigns ↗</Link>}
          >
            {data.activeCampaigns.length === 0 ? (
              <Empty title="Nothing in motion yet">Ask Sarah to launch a campaign and it'll appear here.</Empty>
            ) : (
              <div className="list">
                {data.activeCampaigns.map((c) => (
                  <div className="record" key={c.id}>
                    <div className="lead">
                      <div className="title">{c.name}</div>
                      <div className="sub">{c.objective}</div>
                    </div>
                    <Status value={c.status} />
                  </div>
                ))}
              </div>
            )}
          </Card>

          <div className="stack">
            <Card title="Channels">
              {data.channels.length === 0 ? (
                <Empty title="No channels">Connect one in settings.</Empty>
              ) : (
                <div className="list">
                  {data.channels.map((c) => (
                    <div className="record" key={c.id}>
                      <div className="lead">
                        <div className="title" style={{ fontSize: '0.92rem' }}>{c.name}</div>
                        <div className="sub mono" style={{ textTransform: 'uppercase', fontSize: '0.64rem', letterSpacing: '0.1em' }}>{c.type}</div>
                      </div>
                      <Badge tone={c.status === 'CONNECTED' ? 'green' : c.live ? 'amber' : 'slate'}>
                        {c.live ? humanize(c.status) : 'Mock'}
                      </Badge>
                    </div>
                  ))}
                </div>
              )}
            </Card>

            <Card title="Latest activity">
              {data.recentActivity.length === 0 ? (
                <Empty title="Quiet so far">Sarah's actions will log here.</Empty>
              ) : (
                <div className="list">
                  {data.recentActivity.slice(0, 7).map((a, i) => (
                    <div className="record" key={i} style={{ padding: '0.5rem 0' }}>
                      <span className="sub" style={{ color: 'var(--ink)' }}>{humanize(a.action)}</span>
                      <span className="mono" style={{ fontSize: '0.64rem', color: 'var(--ink-faint)' }}>
                        {new Date(a.at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </div>

        <Card
          title="Upcoming publications"
          actions={<Link className="eyebrow" to="/calendar" style={{ color: 'var(--accent)' }}>Calendar ↗</Link>}
        >
          {data.upcomingPublications.length === 0 ? (
            <Empty title="Nothing scheduled">Approved content will queue here before it goes out.</Empty>
          ) : (
            <div className="list">
              {data.upcomingPublications.slice(0, 8).map((p) => (
                <div className="record" key={p.id}>
                  <div className="lead">
                    <div className="title" style={{ fontSize: '0.92rem' }}>
                      <span className="mono" style={{ textTransform: 'uppercase', fontSize: '0.66rem', letterSpacing: '0.1em', color: 'var(--accent)' }}>{p.platform}</span>
                      <span className="sub" style={{ marginLeft: '0.6rem' }}>{new Date(p.scheduledAt).toLocaleString()}</span>
                    </div>
                    <div className="sub">{p.content}</div>
                  </div>
                  <Status value={p.status} />
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </>
  );
}

function Figure({ n, label }: { n: number; label: string }) {
  return (
    <div className="figure">
      <span className="num">{n}</span>
      <span className="lab">{label}</span>
    </div>
  );
}
