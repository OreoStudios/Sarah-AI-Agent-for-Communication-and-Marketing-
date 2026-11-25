import { useState, type FormEvent, type ReactNode } from 'react';
import { api } from '../api';
import { Card } from '../components/Layout';

type Mode = 'daily' | 'competitors' | 'prospects' | 'creatives' | 'email' | 'performance';
type Output = { title: string; data: unknown };
const tabs: { id: Mode; label: string }[] = [
  { id: 'daily', label: 'Daily strategy' }, { id: 'competitors', label: 'Competitors' },
  { id: 'prospects', label: 'Lead discovery' }, { id: 'creatives', label: 'AI creatives' },
  { id: 'email', label: 'Email campaigns' }, { id: 'performance', label: 'Performance' }
];
type FieldValues = Record<string, string>;
type FieldDef = { key: string; label: string; required?: boolean; type?: string; initial?: string; multiline?: boolean };
function ActionForm({ fields, label, confirm, busy, onSubmit }: {
  fields: FieldDef[]; label: string; confirm?: string; busy: boolean; onSubmit: (data: FieldValues) => Promise<void>
}) {
  const [values, setValues] = useState<FieldValues>(Object.fromEntries(fields.map(f => [f.key, f.initial ?? ''])));
  const [confirmed, setConfirmed] = useState(false);
  async function submit(e: FormEvent) { e.preventDefault(); if (!busy && (!confirm || confirmed)) { await onSubmit(values); setConfirmed(false); } }
  return <form className="stack" onSubmit={submit} style={{ gap: '0.55rem' }}>
    {fields.map(f => <label key={f.key} className="field"><span>{f.label}</span>
      {f.multiline ? <textarea className="input" rows={3} required={f.required} value={values[f.key] ?? ''} onChange={e => setValues(v => ({ ...v, [f.key]: e.target.value }))} />
        : <input className="input" type={f.type ?? 'text'} required={f.required} value={values[f.key] ?? ''} onChange={e => setValues(v => ({ ...v, [f.key]: e.target.value }))} />}
    </label>)}
    {confirm && <label className="check"><input type="checkbox" checked={confirmed} onChange={e => setConfirmed(e.target.checked)} />{confirm}</label>}
    <button className="button" disabled={busy || (!!confirm && !confirmed)} style={{ alignSelf: 'flex-start' }}>{label}</button>
  </form>;
}
export function MarketingWorkspace() {
  const [mode, setMode] = useState<Mode>('daily');
  const [busy, setBusy] = useState(false);
  const [output, setOutput] = useState<Output | null>(null);
  const [error, setError] = useState('');
  async function action(title: string, fn: () => Promise<{ data: unknown }>) {
    setBusy(true); setError(''); setOutput(null);
    try { const r = await fn(); setOutput({ title, data: r.data ?? 'Saved successfully.' }); }
    catch (e) { setError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Action failed. Verify the integration configuration and server logs.'); }
    finally { setBusy(false); }
  }
  const get = (title: string, path: string) => action(title, () => api.get(path));
  const post = (title: string, path: string, body?: unknown) => action(title, () => api.post(path, body));
  const listButton = (label: string, path: string) => <button className="button" disabled={busy} onClick={() => get(label, path)}>{label}</button>;
  let controls: ReactNode;
  if (mode === 'daily') controls = <>
    <p className="muted">Runs the configured daily objective once per Paris day. Draft-only unless auto-publish is enabled on the server.</p>
    <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap' }}><button className="button" disabled={busy} onClick={() => post('Daily strategy run', '/api/v1/marketing/daily')}>Run today's plan</button>{listButton('Run history', '/api/v1/marketing/daily')}</div>
  </>;
  else if (mode === 'competitors') controls = <ActionForm busy={busy} label="Research competitors" fields={[
    { key: 'market', label: 'Market or service', required: true }, { key: 'competitors', label: 'Competitor names (one to five, comma-separated)', required: true },
    { key: 'language', label: 'Report language', initial: 'en' }
  ]} onSubmit={v => post('Competitor report', '/api/v1/marketing/competitors', { market: v.market, competitors: v.competitors.split(',').map(s => s.trim()).filter(Boolean), language: v.language })} />;
  else if (mode === 'prospects') controls = <>
    <p className="muted">Imports public French business records into the CRM, not personal contact details. One run per day.</p>
    <ActionForm busy={busy} label="Discover and qualify companies" fields={[
      { key: 'query', label: 'Business search (name or term)', required: true },
      { key: 'postalCode', label: 'French postal code (optional)' },
      { key: 'activityPrefix', label: 'Target NAF prefix, e.g. 43.22', required: true }
    ]} onSubmit={v => post('Discovery run', '/api/v1/prospects/acquisition', v)} />
    {listButton('Discovery history', '/api/v1/prospects/acquisition')}
  </>;
  else if (mode === 'creatives') controls = <>
    <ActionForm busy={busy} label="Generate image" confirm="I approve a paid image generation request." fields={[
      { key: 'brief', label: 'Creative brief', required: true, multiline: true },
      { key: 'size', label: 'Size (1024x1024, 1024x1536 or 1536x1024)', required: true, initial: '1024x1024' }
    ]} onSubmit={v => post('Generated creative', '/api/v1/marketing/creatives', { ...v, confirmed: true })} />
    {listButton('Creative library', '/api/v1/marketing/creatives')}
  </>;
  else if (mode === 'email') controls = <>
    <p className="muted">Permission-based lists only. Review the full draft and recipient list before sending. Monitor unsubscribe replies.</p>
    <ActionForm key="consent" busy={busy} label="Record consent" confirm="I have documented marketing consent from this recipient." fields={[
      { key: 'email', label: 'Subscriber email', type: 'email', required: true }, { key: 'evidence', label: 'Consent evidence', required: true }
    ]} onSubmit={v => post('Consent recorded', '/api/v1/marketing/email/subscribers', { ...v, confirmed: true })} />
    <ActionForm key="suppress" busy={busy} label="Suppress email" fields={[{ key: 'email', label: 'Opted-out email address', type: 'email', required: true }]} onSubmit={v => post('Suppression saved', '/api/v1/marketing/email/unsubscribe', v)} />
    <ActionForm key="draft" busy={busy} label="Generate email draft" fields={[
      { key: 'subject', label: 'Subject', required: true }, { key: 'objective', label: 'Objective', required: true, multiline: true }, { key: 'language', label: 'Language', initial: 'en' }
    ]} onSubmit={v => post('Email draft — review before sending', '/api/v1/marketing/email/drafts', v)} />
    <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap' }}>{listButton('Email drafts', '/api/v1/marketing/email/drafts')}{listButton('Subscribers', '/api/v1/marketing/email/subscribers')}</div>
    <ActionForm key="send" busy={busy} label="Approve and send" confirm="I reviewed this exact draft, consent, recipient list, and monitored reply-to inbox." fields={[
      { key: 'id', label: 'Reviewed draft UUID', required: true },
      { key: 'recipients', label: 'Consenting recipients, comma-separated', required: true, multiline: true }
    ]} onSubmit={v => post('SMTP submission result', '/api/v1/marketing/email/drafts/' + encodeURIComponent(v.id) + '/send', { confirmed: true, recipients: v.recipients.split(',').map(s => s.trim()).filter(Boolean) })} />
  </>;
  else controls = <>
    <p className="muted">Enter measured, attributable platform data. The optimizer adjusts channel weights using conversion performance, not invented engagement.</p>
    <ActionForm busy={busy} label="Save performance observation" fields={[
      { key: 'publicationId', label: 'Published post UUID', required: true },
      { key: 'impressions', label: 'Cumulative impressions', type: 'number', required: true },
      { key: 'clicks', label: 'Cumulative clicks', type: 'number', required: true },
      { key: 'conversions', label: 'Cumulative conversions', type: 'number', required: true },
      { key: 'source', label: 'Measurement source', required: true }
    ]} onSubmit={v => post('Observation saved', '/api/v1/marketing/performance/observations', { publicationId: v.publicationId, source: v.source, impressions: Number(v.impressions), clicks: Number(v.clicks), conversions: Number(v.conversions) })} />
    <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap' }}><button className="button" disabled={busy} onClick={() => post('Updated channel weights', '/api/v1/marketing/performance/optimize')}>Recompute weights</button>{listButton('Current weights', '/api/v1/marketing/performance')}</div>
  </>;
  const image = output?.data && typeof output.data === 'object' && 'downloadUrl' in output.data ? (output.data as { downloadUrl?: string }).downloadUrl : undefined;
  return <Card title="Marketing workspace">
    <div className="stack" style={{ gap: '1rem' }}>
      <div role="group" aria-label="Marketing tools" style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
        {tabs.map(t => <button key={t.id} type="button" className="button" aria-pressed={mode === t.id} disabled={busy} style={{ opacity: mode === t.id ? 1 : 0.7 }} onClick={() => { setMode(t.id); setOutput(null); setError(''); }}>{t.label}</button>)}
      </div>
      <div key={mode} className="stack" style={{ gap: '0.9rem' }}>{controls}</div>
      {busy && <p role="status">Processing request…</p>}
      {error && <div role="alert" className="error-notice">{error}</div>}
      {output && <div className="stack" style={{ gap: '0.5rem' }}><h3>{output.title}</h3>
        {image?.startsWith('/api/v1/marketing/creatives/') && <a href={image} target="_blank" rel="noreferrer"><img src={image} alt="Generated marketing creative" style={{ maxWidth: '100%', maxHeight: '420px', objectFit: 'contain' }} /></a>}
        <pre style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', maxHeight: '360px', overflow: 'auto', fontSize: '0.8rem' }}>{JSON.stringify(output.data, null, 2)}</pre>
      </div>}
    </div>
  </Card>;
}
