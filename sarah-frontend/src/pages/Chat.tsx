import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { api, type AgentAction, type AuthReply, type ChatOutcome } from '../api';
import { useAuth } from '../auth';
import { PageHeading, humanize } from '../components/Layout';

interface Msg { role: 'USER' | 'ASSISTANT'; content: string; actions?: AgentAction[]; provider?: string; }

// Logout is deterministic and never goes to the agent: Sarah is still the interface.
const LOGOUT = /\b(log\s*out|logout|sign\s*out|log me out)\b/i;

const SUGGESTIONS = [
  'Launch a campaign to promote our Spring Boot services on Discord',
  'Draft a LinkedIn post for restaurants that need a website',
  'What should we market this month?',
];

export function Chat() {
  const [messages, setMessages] = useState<Msg[]>([{
    role: 'ASSISTANT',
    content: "I'm Sarah. Tell me what you'd like to promote and I'll brief the team, draft the content, run it past compliance, and line up anything that's ready to publish.",
  }]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const convId = useRef<string | null>(null);
  const threadRef = useRef<HTMLDivElement>(null);
  const { refresh } = useAuth();

  useEffect(() => {
    threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, busy]);

  async function sendText(text: string) {
    if (!text || busy) return;
    setMessages((m) => [...m, { role: 'USER', content: text }]);
    setInput('');
    setBusy(true);
    try {
      if (LOGOUT.test(text)) {
        // Route logout through the deterministic auth handler, then drop to the guest view.
        const r = await api.post<AuthReply>('/api/auth/chat', { message: text });
        setMessages((m) => [...m, { role: 'ASSISTANT', content: r.data.reply }]);
        setTimeout(() => { refresh(); }, 900);
        return;
      }
      const r = await api.post<ChatOutcome>('/api/v1/chat', { conversationId: convId.current, message: text });
      convId.current = r.data.conversationId;
      setMessages((m) => [...m, { role: 'ASSISTANT', content: r.data.reply, actions: r.data.actions, provider: r.data.llmProvider }]);
    } catch {
      setMessages((m) => [...m, { role: 'ASSISTANT', content: 'Something went wrong reaching the backend. Check that the API is running, then try again.' }]);
    } finally {
      setBusy(false);
    }
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    sendText(input.trim());
  }

  function onKey(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendText(input.trim());
    }
  }

  const fresh = messages.length === 1;

  return (
    <>
      <PageHeading eyebrow="Correspondence" title="Sarah" description="Direct the work in plain language. Her team's steps appear in the margin." />

      <div className="conversation">
        <div className="thread" ref={threadRef}>
          {messages.map((m, i) => (m.role === 'USER'
            ? (
              <div className="turn from-user" key={i}>
                <div className="note-user">{m.content}</div>
              </div>
            )
            : (
              <div className="turn" key={i} style={m.actions && m.actions.length ? undefined : { gridTemplateColumns: '1fr' }}>
                <article className="dispatch">
                  <div className="from">From Sarah</div>
                  <div className="body">{m.content}</div>
                  {m.provider && <div className="via">drafted via {m.provider}</div>}
                </article>
                {m.actions && m.actions.length > 0 && (
                  <aside className="margin">
                    <div className="margin-label">In the margin</div>
                    {m.actions.map((a, j) => (
                      <div className="mark" key={j}>
                        <span className="seq">{String(j + 1).padStart(2, '0')}</span>
                        <span className="who">{a.agent}</span>
                        <span className="what">{humanize(a.action)}</span>
                        {a.detail && <span className="detail">{a.detail}</span>}
                      </div>
                    ))}
                  </aside>
                )}
              </div>
            )
          ))}

          {fresh && !busy && (
            <div className="turn" style={{ gridTemplateColumns: '1fr' }}>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                {SUGGESTIONS.map((s) => (
                  <button key={s} type="button" className="button quiet small" onClick={() => sendText(s)}>{s}</button>
                ))}
              </div>
            </div>
          )}

          {busy && (
            <div className="turn" style={{ gridTemplateColumns: '1fr' }}>
              <article className="dispatch">
                <div className="from">From Sarah</div>
                <div className="body muted">Thinking it through<span className="caret" /></div>
              </article>
            </div>
          )}
        </div>

        <form className="composer" onSubmit={submit}>
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKey}
            rows={1}
            placeholder="Ask Sarah to create a campaign, draft content, or anything marketing…  (Enter to send)"
            aria-label="Message Sarah"
          />
          <button className="button" disabled={busy || !input.trim()} type="submit">Send</button>
        </form>
      </div>
    </>
  );
}
