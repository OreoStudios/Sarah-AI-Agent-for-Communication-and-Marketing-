import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { api, type AuthReply, type AuthStateValue } from '../api';
import { useAuth } from '../auth';
import { SarahMark } from '../components/Layout';
import { preview, previewPages } from '../preview';

interface Line { from: 'sarah' | 'you'; text: string; }

const GREETING = "Hey! I'm Sarah, your AI marketing assistant. 👋 "
  + "Before we start, what's your email address? I'll send a quick code to verify it's you.";

export function GuestGate() {
  const { refresh } = useAuth();
  const [lines, setLines] = useState<Line[]>([{ from: 'sarah', text: GREETING }]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [state, setState] = useState<AuthStateValue>('GUEST');
  const [email, setEmail] = useState<string | null>(null);
  const threadRef = useRef<HTMLDivElement>(null);

  // Restore an in-progress challenge (e.g. after a refresh mid-flow).
  useEffect(() => {
    api.get<AuthReply>('/api/auth/session').then((r) => {
      setState(r.data.state);
      setEmail(r.data.email);
      if (r.data.state === 'CODE_SENT' && r.data.email) {
        setLines([{ from: 'sarah', text: `Welcome back — enter the 6-digit code I sent to ${r.data.email}, or say “resend” for a new one.` }]);
      }
    }).catch(() => { /* backend may be down; keep the greeting */ });
  }, []);

  useEffect(() => {
    threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight, behavior: 'smooth' });
  }, [lines, busy]);

  function apply(reply: AuthReply) {
    setLines((l) => [...l, { from: 'sarah', text: reply.reply }]);
    setState(reply.state);
    setEmail(reply.email);
    if (reply.authenticated) {
      // Let the welcome land, then swap into the workspace.
      setTimeout(() => { refresh(); }, 900);
    }
  }

  async function send(text: string) {
    const msg = text.trim();
    if (!msg || busy) return;
    setLines((l) => [...l, { from: 'you', text: msg }]);
    setInput('');
    setBusy(true);
    try {
      const r = await api.post<AuthReply>('/api/auth/chat', { message: msg });
      apply(r.data);
    } catch {
      setLines((l) => [...l, { from: 'sarah', text: "I couldn't reach the server just now. Give it a moment and try again." }]);
    } finally {
      setBusy(false);
    }
  }

  async function resend() {
    if (busy) return;
    setBusy(true);
    try {
      const r = await api.post<AuthReply>('/api/auth/resend-code');
      apply(r.data);
    } catch {
      /* ignore */
    } finally {
      setBusy(false);
    }
  }

  function onSubmit(e: FormEvent) { e.preventDefault(); send(input); }
  function onKey(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(input); }
  }

  const awaitingCode = state === 'CODE_SENT' || state === 'EXPIRED' || state === 'LOCKED';

  return (
    <div className="gate">
      <div className="onboard">
        <div className="brand" style={{ padding: '0 0 1.2rem' }}>
          <SarahMark />
          <span>Sarah<span className="brand-caption">BY OREO STUDIOS</span></span>
        </div>

        <div className="thread onboard-thread" ref={threadRef}>
          {lines.map((l, i) => (l.from === 'you'
            ? <div className="turn from-user" key={i}><div className="note-user">{l.text}</div></div>
            : (
              <div className="turn" key={i} style={{ gridTemplateColumns: '1fr' }}>
                <article className="dispatch"><div className="body" style={{ fontSize: '1.02rem' }}>{l.text}</div></article>
              </div>
            )
          ))}
          {busy && (
            <div className="turn" style={{ gridTemplateColumns: '1fr' }}>
              <article className="dispatch"><div className="body muted">One moment<span className="caret" /></div></article>
            </div>
          )}
        </div>

        <form className="composer" onSubmit={onSubmit}>
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKey}
            rows={1}
            inputMode={awaitingCode ? 'numeric' : 'email'}
            autoFocus
            placeholder={awaitingCode ? 'Enter your 6-digit code…' : 'your@email.com'}
            aria-label="Message Sarah"
          />
          <button className="button" type="submit" disabled={busy || !input.trim()}>Send</button>
        </form>

        <div className="onboard-foot">
          {awaitingCode
            ? <button type="button" className="linklike" onClick={resend} disabled={busy}>Resend code{email ? ` to ${email}` : ''}</button>
            : <span className="mono" style={{ fontSize: '0.64rem', color: 'var(--ink-faint)', letterSpacing: '0.04em' }}>Passwordless · we only email you a one-time code</span>}
        </div>

        {import.meta.env.DEV && !preview && (
          <nav aria-label="Design previews" className="onboard-previews">
            <span className="eyebrow">Preview without signing in</span>
            <div>{previewPages.map(([path, label]) => <a key={path} className="button quiet small" href={`/preview${path}`}>{label}</a>)}</div>
          </nav>
        )}
      </div>
    </div>
  );
}
