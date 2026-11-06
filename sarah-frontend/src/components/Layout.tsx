import { useState, type ReactNode } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../auth';
import { useTheme } from '../theme';
import { preview } from '../preview';

/** Sarah's monogram — a nib tracing an "S" path, with an ink node. */
export function SarahMark({ className = '' }: { className?: string }) {
  return (
    <svg className={`sarah-mark ${className}`} viewBox="0 0 40 40" fill="none" aria-hidden="true">
      <path d="M24 7H10v14h20v12H16M10 27v6M30 7v6" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
      <circle cx="20" cy="21" r="3.5" fill="currentColor" />
    </svg>
  );
}

// Two intents, pointed only at live routes.
const make: [string, string][] = [
  ['/chat', 'Sarah'],
  ['/campaigns', 'Campaigns'],
  ['/calendar', 'Calendar'],
];
const perspective: [string, string][] = [
  ['/', 'Intelligence'],
  ['/prospects', 'Audience'],
  ['/settings', 'Channels & settings'],
];

export function Layout({ children }: { children: ReactNode }) {
  const { me, logout } = useAuth();
  const { dark, toggle } = useTheme();
  const [menu, setMenu] = useState(false);
  const { pathname } = useLocation();
  const section = [...make, ...perspective].find(([to]) => to === pathname)?.[1] ?? 'Workspace';

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Skip to content</a>

      <aside className="rail">
        <NavLink to="/chat" className="brand" onClick={() => setMenu(false)}>
          <SarahMark />
          <span>Sarah<span className="brand-caption">BY OREO STUDIOS</span></span>
        </NavLink>

        <button
          className="mobile-menu button quiet small"
          aria-expanded={menu}
          aria-controls="workspace-nav"
          onClick={() => setMenu((m) => !m)}
        >
          {menu ? 'Close' : 'Menu'}
        </button>

        <div id="workspace-nav" className={`rail-body ${menu ? 'is-open' : ''}`}>
          <nav aria-label="Workspace">
            <span className="nav-label">Make things happen</span>
            {make.map(([to, name], i) => (
              <NavLink
                key={to}
                to={to}
                onClick={() => setMenu(false)}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                <span className="nav-index">{String(i + 1).padStart(2, '0')}</span>
                {name}
                {to === '/chat' && <span className="nav-arrow" aria-hidden="true">↗</span>}
              </NavLink>
            ))}

            <span className="nav-label second">Keep perspective</span>
            {perspective.map(([to, name]) => (
              <NavLink
                key={to}
                to={to}
                end
                onClick={() => setMenu(false)}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
              >
                {name}
              </NavLink>
            ))}
          </nav>

          <div className="rail-footer">
            <div className="workspace-label">Your workspace</div>
            <div className="account" title={me?.email}>{me?.email}</div>
            <div className="rail-controls">
              <button onClick={toggle} aria-label={`Switch to ${dark ? 'light' : 'dark'} theme`}>
                {dark ? 'Light theme' : 'Dark theme'}
              </button>
              {preview ? <a href="/login">Back to login</a> : <button onClick={logout}>Sign out</button>}
            </div>
          </div>
        </div>
      </aside>

      <div className="main-area">
        <header className="masthead">
          <span>Oreo Studios <span className="separator">/</span> {section}</span>
          <span className="edition">Marketing, with intent</span>
        </header>
        <main id="main-content" tabIndex={-1} className="page">
          {preview && <p className="error-notice" role="status">Design preview · Sample data. Changes are not saved. <a href="/login">Back to login</a></p>}
          {children}
        </main>
        <footer className="page-footer">
          <span>Sarah · a considered approach to marketing.</span>
          <span>Oreo Studios</span>
        </footer>
      </div>
    </div>
  );
}

export function Card({ title, children, actions }: { title?: string; children: ReactNode; actions?: ReactNode }) {
  return (
    <section className="section">
      {(title || actions) && (
        <header className="section-heading">
          {title && <h2>{title}</h2>}
          {actions}
        </header>
      )}
      {children}
    </section>
  );
}

export function Badge({ children, tone = 'slate' }: { children: ReactNode; tone?: string }) {
  return <span className={`status tone-${tone}`}>{children}</span>;
}

export function PageHeading({ eyebrow, title, description, actions }: {
  eyebrow: string; title: string; description?: string; actions?: ReactNode;
}) {
  return (
    <header className="page-heading">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        {description && <p className="page-description">{description}</p>}
      </div>
      {actions && <div className="heading-actions">{actions}</div>}
    </header>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty-state">
      <SarahMark />
      <div><h3>{title}</h3><div className="muted">{children}</div></div>
    </div>
  );
}

export function ErrorNotice({ message, retry }: { message: string | null; retry?: () => void }) {
  if (!message) return null;
  return (
    <div className="error-notice" role="alert">
      <span>{message}</span>
      {retry && <button className="button quiet small" onClick={retry}>Try again</button>}
    </div>
  );
}

export function Loading({ text = 'Sarah is gathering the workspace…' }: { text?: string }) {
  return <div className="loading" role="status"><SarahMark />{text}</div>;
}

export function humanize(value: string) {
  return value.toLowerCase().replace(/_/g, ' ').replace(/^./, (c) => c.toUpperCase());
}

/** Maps a backend status string to a toned pill. */
export function Status({ value }: { value: string }) {
  const tone = /FAILED|REJECTED|CANCELLED|LOST/.test(value) ? 'red'
    : /APPROVED|PUBLISHED|COMPLETED|CONNECTED|WON/.test(value) ? 'green'
    : /REVIEW|PENDING|SCHEDULED|PAUSED|SETUP|RUNNING/.test(value) ? 'amber'
    : 'slate';
  return <Badge tone={tone}>{humanize(value)}</Badge>;
}
