# Frontend design audit

## Existing implementation
React 19, TypeScript (strict), Vite 6, Tailwind 4, React Router 7 and Axios. Local component state; Basic authentication managed by AuthProvider and localStorage. No frontend test runner or lint command exists. Routes: `/`, `/chat`, `/campaigns`, `/calendar`, `/prospects`, `/settings`, `/login`. Same-origin `/api/v1` calls are proxied by Vite/nginx.

## Concrete findings
- `components/Layout.tsx`: fixed 256px sidebar leaves little room on phones; every route has equal priority. Unicode icons lack a consistent visual language. Card forces border, shadow, rounding and padding onto unrelated information.
- `index.css`: no semantic tokens, type scale, shared interaction states or motion policy. System sans at every level. Slate/indigo utility colors scattered throughout pages.
- `Chat.tsx`: initial assistant message substitutes for a welcome experience; single-line input limits composition; all replies are bubbles; returned campaignId is discarded; agent outcomes have little hierarchy. Failed requests become assistant messages, confusing operational errors with replies.
- `Campaigns.tsx`: card stack obscures an index; clickable div is not keyboard accessible. Draft loading looks like an empty state. No filtering, sorting or direct detail links. Form nests checkbox labels inside another label.
- `Dashboard.tsx`: four equal KPI cards followed by four equal panels. No clear next step. Existing API supplies operational counts, not engagement or conversion metrics.
- `Calendar.tsx`: three flat lists truncate the content, do not group dates, and provide no feedback on failed retry/cancel requests.
- `Prospects.tsx`: placeholder-only fields; unlabeled status selectors; unhandled create/status/suppress failures; wide table without a mobile presentation.
- `Settings.tsx`: channel secrets visible as plain text, placeholder-only controls; autonomy mutation failures unhandled. Mock integrations must remain explicitly labeled.
- `Login.tsx`: generic floating card; theme initialized only inside authenticated layout; login failures all reported as invalid credentials.

## Preservation and capability boundaries
Keep every existing route, endpoint, payload, auth guard and business action. Add Studio as a read-only review surface using campaign draft endpoints. Add Activity using dashboard audit events. Keep `/` as the operational overview, rename it Intelligence, and make Sarah the most prominent navigation destination. Do not invent engagement metrics, live intermediate agent events, attachments, approval actions, editing or history without backend support. Conversation actions are completed operational outcomes, not hidden reasoning.

## Validation plan
Run TypeScript and production build. Attempt browser verification of desktop/mobile and both themes, using explicit test fixtures only if the backend is unavailable. Check navigation, empty/error/loading states, form labels, campaign detail and chat submission. Document any environment restrictions instead of claiming live integration coverage.
