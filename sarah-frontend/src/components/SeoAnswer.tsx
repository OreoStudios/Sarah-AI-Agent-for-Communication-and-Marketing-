import type { ReactNode } from 'react';

/**
 * Small, safe formatter for text returned by a web-search AI provider.
 * React escapes all text; no HTML evaluation and no extra Markdown dependency.
 */
function Inline({ value }: { value: string }) {
  const parts = value.split(/(\*\*[^*]+\*\*|\[[^\]]+\]\(https?:\/\/[^\s)]+\))/g);
  return <>{parts.map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**')) return <strong key={i}>{part.slice(2, -2)}</strong>;
    const link = /^\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)$/.exec(part);
    if (link) return <a key={i} href={link[2]} target="_blank" rel="noopener noreferrer">{link[1]}</a>;
    return <span key={i}>{part}</span>;
  })}</>;
}

function Table({ lines }: { lines: string[] }) {
  const rows = lines.map(line => line.split('|').slice(1, -1).map(cell => cell.trim()));
  const good = rows.filter(row => !row.every(cell => /^:?-{3,}:?$/.test(cell)));
  if (!good.length) return null;
  return <div style={{ overflowX: 'auto', marginTop: '1rem', marginBottom: '1rem' }}>
    <table>
      <thead><tr>{good[0].map((c, i) => <th key={i}><Inline value={c}/></th>)}</tr></thead>
      <tbody>{good.slice(1).map((row, i) => <tr key={i}>{row.map((c, j) => <td key={j}><Inline value={c}/></td>)}</tr>)}</tbody>
    </table>
  </div>;
}

export function SeoAnswer({ answer }: { answer: string }) {
  // Some providers escape Markdown punctuation even within a natural-language answer.
  const clean = answer.replace(/\\([*|[\]])/g, '$1');
  const paragraphs = clean.split(/\n\s*\n/).map(s => s.trim()).filter(Boolean);
  const blocks: ReactNode[] = paragraphs.map((p, i) => {
    const lines = p.split('\n').map(x => x.trim()).filter(Boolean);
    if (lines.length > 1 && lines.every(x => x.startsWith('|') && x.endsWith('|')))
      return <Table key={i} lines={lines}/>;
    if (lines.every(x => /^[-*] /.test(x)))
      return <ul key={i}>{lines.map((x, n) => <li key={n}><Inline value={x.slice(2)}/></li>)}</ul>;
    return <p key={i} style={{ whiteSpace: 'pre-line' }}><Inline value={p}/></p>;
  });
  return <div className="seo-answer">{blocks}</div>;
}
