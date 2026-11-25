import { Fragment, useEffect, useState, type FormEvent } from 'react';
import { api, type Campaign, type Draft, type Channel, type Publication } from '../api';
import { Badge, Card, Empty, PageHeading, Status } from '../components/Layout';

const PLATFORMS = ['linkedin', 'instagram', 'facebook', 'discord', 'reddit', 'website'];
const PIPELINE = ['CREATED', 'PLANNED', 'REVIEWED', 'SCHEDULED', 'RUNNING', 'COMPLETED'];

export function Campaigns() {
  const [list, setList] = useState<Campaign[]>([]);
  const [open, setOpen] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Record<string, Draft[]>>({});
  const [creating, setCreating] = useState(false);

  function reload() {
    api.get<Campaign[]>('/api/v1/campaigns').then((r) => setList(r.data));
  }
  useEffect(reload, []);

  async function toggle(id: string) {
    if (open === id) { setOpen(null); return; }
    setOpen(id);
    if (!drafts[id]) {
      const r = await api.get<Draft[]>(`/api/v1/campaigns/${id}/drafts`);
      setDrafts((d) => ({ ...d, [id]: r.data }));
    }
  }

  return (
    <>
      <PageHeading
        eyebrow="Campaigns"
        title="The work in flight"
        description="Every campaign Sarah is running, from first brief to published."
        actions={<button className="button" onClick={() => setCreating((c) => !c)}>{creating ? 'Close' : '+ New campaign'}</button>}
      />

      <div className="stack">
        {creating && <CreateForm onCreated={() => { setCreating(false); reload(); }} />}

        {list.length === 0 ? (
          <Card><Empty title="No campaigns yet">Ask Sarah in the chat to launch one, or create it by hand.</Empty></Card>
        ) : (
          list.map((c) => (
            <section className="section" key={c.id}>
              <header
                className="section-heading"
                style={{ cursor: 'pointer', marginBottom: open === c.id ? '1.1rem' : 0 }}
                onClick={() => toggle(c.id)}
              >
                <div style={{ minWidth: 0 }}>
                  <h2 style={{ fontSize: '1.2rem' }}>{c.name}</h2>
                  <p className="muted" style={{ margin: '0.3rem 0 0', fontSize: '0.9rem' }}>{c.objective}</p>
                </div>
                <Status value={c.status} />
              </header>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.8rem' }}>
                <Pipeline status={c.status} />
                <div style={{ display: 'flex', gap: '0.3rem', flexWrap: 'wrap' }}>
                  {c.platforms.map((p) => <Badge key={p} tone="indigo">{p}</Badge>)}
                  <Badge>{c.language}</Badge>
                </div>
              </div>

              {open === c.id && (
                <div className="stack" style={{ gap: '0.8rem', marginTop: '1.2rem' }}>
                  {(drafts[c.id] ?? []).length === 0 && <p className="muted" style={{ fontSize: '0.88rem' }}>No drafts generated yet.</p>}
                  {(drafts[c.id] ?? []).map((d) => (
                    <div className="draft" key={d.id}>
                      <div className="head">
                        <Badge tone="indigo">{d.platform}</Badge>
                        <span style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                          {d.complianceScore != null && <span className="mono" style={{ fontSize: '0.68rem', color: 'var(--ink-faint)' }}>score {d.complianceScore}</span>}
                          <Status value={d.status} />
                        </span>
                      </div>
                      {d.title && <div style={{ fontWeight: 600, marginBottom: '0.3rem' }}>{d.title}</div>}
                      <div className="copy">{d.body}</div>
                      {d.complianceNotes && <div className="notes">{d.complianceNotes}</div>}
                      {d.status === 'APPROVED' && ['instagram', 'reddit'].includes(d.platform.toLowerCase()) && <SocialReview draft={d} />}
                    </div>
                  ))}
                </div>
              )}
            </section>
          ))
        )}
      </div>
    </>
  );
}

function Pipeline({ status }: { status: string }) {
  const idx = PIPELINE.indexOf(status);
  if (idx === -1) return <span className="mono" style={{ fontSize: '0.62rem', letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--neg)' }}>{status.toLowerCase()}</span>;
  return (
    <div className="pipeline">
      {PIPELINE.map((stage, i) => (
        <Fragment key={stage}>
          <span className={`stage ${i < idx ? 'done' : ''} ${i === idx ? 'current' : ''}`}>
            <span className="dot" />
            {String(i + 1).padStart(2, '0')} {stage.toLowerCase()}
          </span>
          {i < PIPELINE.length - 1 && <span className="rail-sep" aria-hidden="true">·</span>}
        </Fragment>
      ))}
    </div>
  );
}

function CreateForm({ onCreated }: { onCreated: () => void }) {
  const [name, setName] = useState('');
  const [objective, setObjective] = useState('');
  const [audience, setAudience] = useState('SMBs and software teams');
  const [platforms, setPlatforms] = useState<string[]>(['discord']);
  const [language, setLanguage] = useState('en');
  const [error, setError] = useState<string | null>(null);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.post('/api/v1/campaigns', { name, objective, targetAudience: audience, platforms, language, budgetCents: 0 });
      onCreated();
    } catch (err: unknown) {
      setError((err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to create campaign');
    }
  }

  return (
    <Card title="New campaign">
      <form className="stack" style={{ gap: '0.9rem' }} onSubmit={submit}>
        {error && <div className="error-notice" role="alert"><span>{error}</span></div>}
        <label className="field"><span>Name</span><input className="input" required value={name} onChange={(e) => setName(e.target.value)} /></label>
        <label className="field"><span>Objective</span><textarea className="input" required rows={2} value={objective} onChange={(e) => setObjective(e.target.value)} /></label>
        <label className="field"><span>Target audience</span><input className="input" value={audience} onChange={(e) => setAudience(e.target.value)} /></label>
        <div className="field">
          <span>Platforms</span>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.9rem', paddingTop: '0.2rem' }}>
            {PLATFORMS.map((p) => (
              <label key={p} className="check">
                <input type="checkbox" checked={platforms.includes(p)}
                  onChange={(e) => setPlatforms((cur) => e.target.checked ? [...cur, p] : cur.filter((x) => x !== p))} />
                {p}
              </label>
            ))}
          </div>
        </div>
        <label className="field" style={{ maxWidth: '12rem' }}>
          <span>Language</span>
          <select className="input" value={language} onChange={(e) => setLanguage(e.target.value)}>
            <option value="en">English</option>
            <option value="fr">French</option>
          </select>
        </label>
        <button className="button" style={{ alignSelf: 'flex-start' }}>Create campaign</button>
      </form>
    </Card>
  );
}

function SocialReview({ draft }: { draft: Draft }) {
  const [channels, setChannels] = useState<Channel[]>([]);
  const [channel, setChannel] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  useEffect(() => {
    api.get<Channel[]>('/api/v1/connectors').then(r => setChannels(r.data.filter(c =>
      c.type.toLowerCase() === draft.platform.toLowerCase() && c.live && c.status === 'CONNECTED')))
      .catch(() => setMessage('Could not load channels.'));
  }, [draft.platform]);
  async function approve() {
    setBusy(true); setMessage('');
    try {
      const r = await api.post<Publication>('/api/v1/marketing/reviewed-social', {
        campaignId: draft.campaignId, draftId: draft.id, channelId: channel, confirmed
      });
      setMessage('Publication record: ' + r.data.status + '. Check Calendar for delivery.');
      setConfirmed(false);
    } catch { setMessage('Could not schedule this draft. Check channel status and campaign.'); }
    finally { setBusy(false); }
  }
  return <div className="stack" style={{ gap: '0.5rem', marginTop: '0.7rem' }}>
    <label className="field"><span>Review and select an authorized channel</span>
      <select className="input" value={channel} onChange={e => { setChannel(e.target.value); setConfirmed(false); }}>
        <option value="">Choose channel</option>
        {channels.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
      </select>
    </label>
    <label className="check"><input type="checkbox" checked={confirmed} onChange={e => setConfirmed(e.target.checked)} />
      I reviewed this draft and the channel's configured media or title, and confirm permission and community rules.</label>
    <button className="button" disabled={!confirmed || !channel || busy} onClick={approve}>Approve and schedule</button>
    {message && <p role="status">{message}</p>}
  </div>;
}
