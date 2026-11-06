import { useEffect, useState, type FormEvent } from 'react';
import { api, type Prospect } from '../api';
import { Badge, Card, Empty, PageHeading, Status } from '../components/Layout';

const STATUSES = ['NEW', 'CONTACTED', 'REPLIED', 'QUALIFIED', 'WON', 'LOST'];

export function Prospects() {
  const [list, setList] = useState<Prospect[]>([]);
  const [form, setForm] = useState({ businessName: '', website: '', sector: '', location: '' });

  function reload() { api.get<Prospect[]>('/api/v1/prospects').then((r) => setList(r.data)); }
  useEffect(reload, []);

  async function create(e: FormEvent) {
    e.preventDefault();
    await api.post('/api/v1/prospects', form);
    setForm({ businessName: '', website: '', sector: '', location: '' });
    reload();
  }
  async function setStatus(id: string, status: string) { await api.post(`/api/v1/prospects/${id}/status`, null, { params: { status } }); reload(); }
  async function suppress(id: string) { await api.post(`/api/v1/prospects/${id}/suppress`); reload(); }

  return (
    <>
      <PageHeading eyebrow="Audience" title="Who's worth a conversation" description="Prospects Sarah is tracking, ranked by fit. Opt-outs are honoured and locked." />

      <div className="stack">
        <Card title="Add a prospect">
          <form className="grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)', alignItems: 'end' }} onSubmit={create}>
            <label className="field"><span>Business</span><input className="input" required value={form.businessName} onChange={(e) => setForm({ ...form, businessName: e.target.value })} /></label>
            <label className="field"><span>Website</span><input className="input" value={form.website} onChange={(e) => setForm({ ...form, website: e.target.value })} /></label>
            <label className="field"><span>Sector</span><input className="input" value={form.sector} onChange={(e) => setForm({ ...form, sector: e.target.value })} /></label>
            <label className="field"><span>Location</span><input className="input" value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></label>
            <button className="button" style={{ gridColumn: '1 / -1', justifySelf: 'start' }}>Add prospect</button>
          </form>
        </Card>

        <Card title={`Prospects · ${list.length}`}>
          {list.length === 0 ? (
            <Empty title="No prospects yet">Add one above, or let the prospecting agent fill this in.</Empty>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table className="data-table">
                <thead>
                  <tr><th>Business</th><th>Sector</th><th>Location</th><th>Fit</th><th>Status</th><th aria-label="actions" /></tr>
                </thead>
                <tbody>
                  {list.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <span style={{ fontWeight: 600 }}>{p.businessName}</span>
                        {p.suppressed && <span style={{ marginLeft: '0.5rem' }}><Badge tone="red">suppressed</Badge></span>}
                      </td>
                      <td className="muted">{p.sector ?? '—'}</td>
                      <td className="muted">{p.location ?? '—'}</td>
                      <td><Badge tone={p.leadScore >= 70 ? 'green' : 'amber'}>{p.leadScore}</Badge></td>
                      <td>
                        {p.suppressed
                          ? <Status value={p.contactStatus} />
                          : (
                            <select className="input" style={{ padding: '0.25rem 0.4rem', fontSize: '0.78rem', width: 'auto' }}
                              value={p.contactStatus} onChange={(e) => setStatus(p.id, e.target.value)}>
                              {STATUSES.map((s) => <option key={s}>{s}</option>)}
                            </select>
                          )}
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        {!p.suppressed && <button className="button danger small" onClick={() => suppress(p.id)}>Suppress</button>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>
    </>
  );
}
