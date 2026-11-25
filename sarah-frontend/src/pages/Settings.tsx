import { useEffect, useState, type FormEvent } from 'react';
import { api, type Autonomy, type Channel } from '../api';
import { Badge, Card, Empty, PageHeading, humanize } from '../components/Layout';

export function Settings() {
  const [channels, setChannels] = useState<Channel[]>([]);
  const [autonomy, setAutonomy] = useState<Autonomy | null>(null);
  const [type, setType] = useState('discord');
  const [name, setName] = useState('');
  const [secret, setSecret] = useState('');
  const [target, setTarget] = useState('');
  const [extra, setExtra] = useState('');
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
      const config = type === 'instagram' ? { accountId: target, imageUrl: extra }
        : type === 'reddit' ? { subreddit: target, title: extra }
        : type === 'linkedin' ? { author: target }
        : type === 'facebook' ? { pageId: target }
        : type === 'website' ? { username: target } : null;
      const result = await api.post<Channel>('/api/v1/connectors', { type, name, secret, config: config ? JSON.stringify(config) : null });
      setMsg(result.data.status === 'CONNECTED' ? 'Channel added.' : 'Channel saved but not connected. Check server flags, permissions and credentials.');
      setName(''); setSecret(''); setTarget(''); setExtra('');
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
              <select className="input" value={type} onChange={(e) => { setType(e.target.value); setTarget(''); setExtra(''); setSecret(''); }}>
                <option value="discord">Discord — live</option>
                <option value="linkedin">LinkedIn — opt-in</option>
                <option value="facebook">Facebook — opt-in</option>
                <option value="instagram">Instagram — opt-in</option>
                <option value="reddit">Reddit — opt-in</option>
                <option value="website">Website — opt-in</option>
              </select>
            </label>
            <label className="field"><span>Name</span><input className="input" required value={name} onChange={(e) => setName(e.target.value)} /></label>
            <label className="field" style={{ gridColumn: 'span 2' }}><span>Secret · token or webhook URL</span><input className="input" type="password" autoComplete="off" required value={secret} onChange={(e) => setSecret(e.target.value)} /></label>
            {type !== 'discord' && <label className="field" style={{ gridColumn: 'span 2' }}><span>{({ instagram: 'Instagram professional account ID', reddit: 'Approved subreddit (without r/)', linkedin: 'Author URN', facebook: 'Page ID', website: 'WordPress username' } as Record<string, string>)[type] ?? 'Target'}</span><input className="input" required value={target} onChange={(e) => setTarget(e.target.value)} /></label>}
            {(type === 'instagram' || type === 'reddit') && <label className="field" style={{ gridColumn: 'span 2' }}><span>{type === 'instagram' ? 'Approved public JPEG URL' : 'Approved post title'}</span><input className="input" required value={extra} onChange={(e) => setExtra(e.target.value)} /></label>}
            <button className="button" style={{ gridColumn: '1 / -1', justifySelf: 'start' }}>Add channel</button>
          </form>
          <p className="muted" style={{ fontSize: '0.8rem', marginTop: '0.8rem' }}>
            Discord is live by default. Other connectors require server-side enablement and authorized credentials. Instagram and Reddit require review in Campaigns before scheduling. Use a unique channel name describing the approved media or subreddit and title.
          </p>
        </Card>
      </div>
    </>
  );
}
