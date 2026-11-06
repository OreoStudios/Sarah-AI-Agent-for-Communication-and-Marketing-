import { useEffect, useState, type FormEvent } from 'react';
import { api, type Autonomy, type Channel } from '../api';
import { Badge, Card, Empty, PageHeading, humanize } from '../components/Layout';

export function Settings() {
  const [channels, setChannels] = useState<Channel[]>([]);
  const [autonomy, setAutonomy] = useState<Autonomy | null>(null);
  const [type, setType] = useState('discord');
  const [name, setName] = useState('');
  const [secret, setSecret] = useState('');
  const [msg, setMsg] = useState<string | null>(null);

  function reload() {
    api.get<Channel[]>('/api/v1/connectors').then((r) => setChannels(r.data));
    api.get<Autonomy>('/api/v1/autonomy').then((r) => setAutonomy(r.data));
  }
  useEffect(reload, []);

  async function addChannel(e: FormEvent) {
    e.preventDefault();
    setMsg(null);
    try {
      await api.post('/api/v1/connectors', { type, name, secret, config: null });
      setName(''); setSecret('');
      reload();
    } catch {
      setMsg('Failed to add channel. Check the secret and try again.');
    }
  }

  async function toggleKill() {
    if (!autonomy) return;
    await api.post(`/api/v1/autonomy/${autonomy.enabled ? 'pause' : 'resume'}`);
    reload();
  }

  return (
    <>
      <PageHeading eyebrow="Channels & settings" title="The controls" description="One switch stops everything. Below it, the channels Sarah is allowed to publish to." />

      <div className="stack">
        <section className="section" style={{ borderColor: autonomy && !autonomy.enabled ? 'color-mix(in srgb, var(--neg) 40%, var(--line))' : undefined }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1.5rem', flexWrap: 'wrap' }}>
            <div>
              <p className="eyebrow">Autonomy</p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', margin: '0.5rem 0' }}>
                <span className={`nib ${autonomy?.enabled ? 'live' : 'paused'}`} />
                <h2 style={{ fontSize: '1.4rem' }}>{autonomy?.enabled ? 'Sarah is working' : 'All activity paused'}</h2>
              </div>
              {autonomy && (
                <p className="mono" style={{ fontSize: '0.68rem', color: 'var(--ink-faint)', letterSpacing: '0.05em' }}>
                  {autonomy.dailyPostLimit} posts/day · {autonomy.perPlatformCooldownMinutes}m cooldown · {autonomy.maxLlmCallsPerRun} LLM calls/run
                </p>
              )}
            </div>
            {autonomy && (
              <button className={autonomy.enabled ? 'button danger' : 'button'} onClick={toggleKill}>
                {autonomy.enabled ? 'Pause all activity' : 'Resume'}
              </button>
            )}
          </div>
        </section>

        <Card title="Connected channels">
          {msg && <div className="error-notice" role="alert" style={{ marginBottom: '1rem' }}><span>{msg}</span></div>}
          {channels.length === 0 ? (
            <Empty title="No channels yet">Add a Discord webhook below to publish for real.</Empty>
          ) : (
            <div className="list">
              {channels.map((c) => (
                <div className="record" key={c.id}>
                  <div className="lead">
                    <div className="title" style={{ fontSize: '0.95rem' }}>
                      {c.name} <span className="mono muted" style={{ fontSize: '0.64rem', letterSpacing: '0.08em', textTransform: 'uppercase' }}>· {c.type}</span>
                    </div>
                    {c.setupHint && <div className="sub" style={{ color: 'var(--warn)' }}>{c.setupHint}</div>}
                  </div>
                  <Badge tone={c.status === 'CONNECTED' ? 'green' : c.live ? 'amber' : 'slate'}>{c.live ? humanize(c.status) : 'Mock'}</Badge>
                </div>
              ))}
            </div>
          )}

          <form className="grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)', alignItems: 'end', marginTop: '1.3rem' }} onSubmit={addChannel}>
            <label className="field">
              <span>Platform</span>
              <select className="input" value={type} onChange={(e) => setType(e.target.value)}>
                <option value="discord">Discord — live</option>
                <option value="linkedin">LinkedIn — mock</option>
                <option value="facebook">Facebook — mock</option>
                <option value="instagram">Instagram — mock</option>
                <option value="reddit">Reddit — mock</option>
                <option value="website">Website — mock</option>
              </select>
            </label>
            <label className="field"><span>Name</span><input className="input" required value={name} onChange={(e) => setName(e.target.value)} /></label>
            <label className="field" style={{ gridColumn: 'span 2' }}><span>Secret · e.g. Discord webhook URL</span><input className="input" required value={secret} onChange={(e) => setSecret(e.target.value)} /></label>
            <button className="button" style={{ gridColumn: '1 / -1', justifySelf: 'start' }}>Add channel</button>
          </form>
          <p className="muted" style={{ fontSize: '0.8rem', marginTop: '0.8rem' }}>
            Discord webhooks publish for real. Other platforms are mock adapters until their OAuth credentials and platform approval are in place —
            Sarah reports that honestly rather than pretending a post went out.
          </p>
        </Card>
      </div>
    </>
  );
}
