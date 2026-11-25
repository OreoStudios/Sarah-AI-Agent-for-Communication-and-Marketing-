# Sarah AI Agent

**Autonomous multi-agent marketing platform for Oreo Studios.**

Sarah is a modular-monolith marketing assistant: she turns a natural-language instruction
into a marketing campaign, generates platform-specific content with specialist agents,
runs it through deterministic compliance checks, and schedules compliant publications to
authorized channels — with a kill switch, rate limits, idempotency, retries, and a full
audit trail.

> This is an evolving marketing platform with opt-in publishing, SEO, B2B discovery, analytics and semantic memory integrations.
> It is built to automate **legitimate** marketing workflows, respecting platform policies,
> privacy, and owner-defined limits. It does not spam, scrape restricted data, or evade
> platform restrictions.

---

## Tech stack (verified compatible versions)

| Layer      | Choice |
|------------|--------|
| Backend    | Java 21 (compiled `--release 21`, runs on JDK 25), **Spring Boot 3.5.14** |
| AI         | **Spring AI 1.0.6** (Anthropic Claude), with an offline mock provider |
| Modularity | **Spring Modulith 1.4.1** (boundaries verified in tests) |
| Data       | PostgreSQL + **pgvector** (`pgvector/pgvector:pg16`), Flyway migrations |
| Cache      | Redis 7 |
| Frontend   | React 19 + TypeScript + Vite 6 + **Tailwind CSS v4** |
| Deploy     | Docker Compose (Coolify-compatible) |
| Observ.    | Actuator, Micrometer + Prometheus, structured logging, OpenAPI/Swagger |

---

## Repository layout

```
Sarah AI Agent/
├── docker-compose.yml          # full stack: db, redis, backend, frontend
├── .env.example                # copy to .env
├── sarah-backend/              # Spring Boot modular monolith (Maven)
│   └── src/main/java/com/oreostudios/sarah/
│       ├── agent/              # orchestrator + content + compliance agents
│       ├── campaign/           # campaigns, drafts, workflow state machine
│       ├── publishing/         # idempotent scheduling, retry/backoff, limits
│       ├── connector/          # capability-based connectors (Discord and opt-in real adapters)
│       ├── autonomy/           # kill switch + limits (persisted)
│       ├── scheduling/         # background publisher + workflow tasks
│       ├── memory/             # conversation + semantic memory (pgvector)
│       ├── brand/ security/ audit/ llm/ prospecting/  # supporting modules
│       ├── dashboard/          # read-only aggregator for the home screen
│       └── shared/ config/     # OPEN modules (shared types, configuration)
└── sarah-frontend/             # React dashboard
```

The backend is a **Spring Modulith** application. Every business module follows a hexagonal
layout — a public `api/` package (a `@NamedInterface`: facade interface + DTOs/events/enums)
over `internal/{domain, application, infrastructure/web}`. Other modules may reference only a
module's `:: api`; each module's `package-info.java` declares its `@ApplicationModule` with an
explicit `allowedDependencies` allow-list, and `shared`/`config` are `OPEN` modules.
Boundaries are enforced by a test (`ModularityTest` runs `ApplicationModules.verify()`), so a
reach into another module's `internal` packages, an undeclared dependency, or a cycle fails the
build. All REST endpoints are versioned under `/api/v1/**`.

---

## Quick start (Docker Compose)

```bash
cp .env.example .env
# optional: generate a real token-encryption key
#   openssl rand -base64 32   -> SARAH_TOKEN_KEY in .env
docker compose up --build
```

- Dashboard: <http://localhost:8088>
- Backend API + Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>
- Log in with the seeded owner (`SARAH_ADMIN_EMAIL` / `SARAH_ADMIN_PASSWORD`, default
  `owner@oreostudios.com` / `change-me-now`). **Change the password after first login.**

The platform runs in mock LLM mode by default (`SARAH_LLM_PROVIDER=mock`): no API key
required. Set `SARAH_LLM_PROVIDER=anthropic` and `ANTHROPIC_API_KEY=…` for live generation
with Claude.

## Local development (without Docker)

```bash
# 1. Postgres with pgvector + Redis
docker run -d --name sarah-db -p 5432:5432 \
  -e POSTGRES_DB=sarah -e POSTGRES_USER=sarah -e POSTGRES_PASSWORD=sarah \
  pgvector/pgvector:pg16
docker run -d --name sarah-redis -p 6379:6379 redis:7-alpine

# 2. Backend
cd sarah-backend && mvn spring-boot:run

# 3. Frontend (proxies /api -> localhost:8080)
cd sarah-frontend && npm install && npm run dev   # http://localhost:5173
```

---

## Connecting a real Discord channel (actually publishes)

1. In Discord: *Server Settings → Integrations → Webhooks → New Webhook*, choose a channel,
   copy the webhook URL.
2. In Sarah: **Settings → Connected channels**, type `Discord`, give it a name, paste the
   webhook URL as the secret, **Add channel**. The webhook URL is encrypted at rest
   (AES-256-GCM) and never returned by the API.
3. Ask Sarah (Chat) to *"launch a campaign to promote our Spring Boot services on Discord"*.
   Approved content is scheduled; the background scheduler publishes it (respecting the kill
   switch, daily limit, and cooldown). The real Discord message id/URL is recorded.

The SSRF guard restricts outbound publishing to an allow-list (`discord.com` by default,
configurable via `SARAH_OUTBOUND_HOSTS`).

---

## What is real vs. mock

| Channel | Status | Notes |
|---------|--------|-------|
| Discord | Live adapter | Authorized webhook. |
| LinkedIn, Facebook | Opt-in real adapters | Official text-post APIs; approved tokens and target configuration. |
| Instagram | Opt-in real adapter | Professional account, approved token and public JPEG URL; owner-reviewed single-image posts. |
| Reddit | Opt-in real adapter | Approved Reddit API access, user token and authorized subreddit; owner-reviewed text posts with automation disclosure. |
| Website | Opt-in real adapter | WordPress REST API; application password and authorized site. |
| Google Search Console | Opt-in real read-only integration | Authorized property and OAuth refresh token. |
| B2B data sources | Real read-only discovery endpoint | Public French company registry; no automatic outreach. |

All opt-in connectors remain mock/disabled until enabled and configured. Live credentials and provider approval must be tested before production use. See [reviewed social publishing](docs/REVIEWED-SOCIAL-PUBLISHING.md) for setup and limits.

Missing credentials/approval are surfaced as a **setup requirement** (`NEEDS_SETUP`), never a
fake success — see `ConnectorService` and the integration test `unsupportedChannelIsReportedNotFaked`.

---

## Conversational authentication (Sarah is the sign-in)

There is no login form. A visitor meets Sarah, introduces themselves by email, and verifies with a
6-digit code — all in chat. It is passwordless email-OTP backed by a real backend:

- **Deterministic, not AI.** Email validation, OTP generation/verification, session creation and
  logout are plain Java in the `auth` module (`ConversationalAuthService`, `OtpService`,
  `SessionService`, `UserAccountService`, `EmailDeliveryService`). The LLM is never invoked for auth
  and **codes are never passed to it** — so prompt injection can't bypass the gate.
- **Codes.** Cryptographically secure 6-digit codes, stored only as an **HMAC-SHA256** (never
  plaintext), **5-minute expiry**, **single-use**, max-attempt lockout, resend cooldown, and
  per-email/per-IP issue rate limits. Delivery failures never record a code as "sent".
- **Sessions.** An **HttpOnly, SameSite session cookie** is the source of truth (no tokens in
  localStorage); the session id is **rotated** on login; **CSRF** uses the cookie/double-submit
  pattern (axios echoes `XSRF-TOKEN` automatically). Logout invalidates the server session —
  conversationally ("Sarah, log me out") or via the sign-out control.
- **Guests are sandboxed.** `/api/auth/**` is the only route a guest can reach; everything under
  `/api/v1/**` requires an authenticated session, so guests can't touch campaigns, tools, or data.
- **Email delivery.** `sarah.auth.email.provider=log` (default) is a dev adapter that sends nothing
  and — deliberately — **never logs the code**. Set `provider=smtp` with `spring.mail.*` (e.g. a
  local Mailhog) for real delivery. Tests use a capturing adapter so the code never touches logs or
  the API.

Endpoints: `GET /api/auth/session`, `POST /api/auth/chat`, `/request-code`, `/verify-code`,
`/resend-code`, `/logout`. The seeded owner (`app_user`) still works over HTTP Basic for admin/API use.

> Production checklist: set `SARAH_AUTH_HMAC_SECRET` (stable), `SARAH_COOKIE_SECURE=true` behind
> HTTPS, and a real SMTP provider. The rate limiter is in-memory (single instance) — back it with
> Redis when running multiple replicas.

## Tests

```bash
cd sarah-backend && mvn test
```

See GitHub Actions for the current test result.

- Unit/slice tests always run: compliance rules, campaign state machine, token encryption,
  SSRF guard, the real Discord HTTP publish path (against an embedded server), and
  **Spring Modulith boundary verification**.
- `SarahIntegrationTest` (Testcontainers + real pgvector Postgres) covers the end-to-end
  flow: NL → campaign → drafts → compliance → idempotent scheduling → kill-switch block →
  publish-once → retry-without-duplicate → unsupported-channel reporting. It is annotated
  `@Testcontainers(disabledWithoutDocker = true)`, so it **skips automatically when Docker is
  unavailable** and runs when Docker is healthy (`mvn verify` with a working Docker daemon).

> Note: on the build machine used here, Docker Desktop's engine returned HTTP 500
> (a local CLI/engine API-version mismatch), so the 4 Testcontainers tests skipped. Restart
> Docker Desktop and re-run `mvn test` to execute them.

---

## Autonomy, safety & security

- **Kill switch** — `POST /api/autonomy/pause` (OWNER) stops all autonomous publishing
  immediately; persisted in the DB and checked before every publish.
- **Limits** — daily post limit, per-platform cooldown, max LLM calls per run, agent-hop cap
  (loop protection). All enforced deterministically in Java.
- **Idempotency** — every publication has a unique idempotency key; retries never double-post.
- **Workflow states** — `CREATED → PLANNED → REVIEWED → SCHEDULED → RUNNING → COMPLETED`
  plus `AWAITING_APPROVAL / FAILED / CANCELLED / PAUSED`, with validated transitions.
- **Compliance** — the Compliance Agent *advises*; the approval decision is deterministic Java.
- **Security** — Spring Security + RBAC (OWNER/OPERATOR/VIEWER), BCrypt, encrypted connector
  tokens, SSRF-restricted outbound HTTP, prompt-injection guard (untrusted content is wrapped
  and never treated as instructions), no public unauthenticated publishing endpoints.
- **Audit** — every meaningful action is written to `audit_log`.

---

## Advanced marketing workflows

The owner can configure these optional workflows using Docker Compose variables in .env.example.
- [Live competitor research](docs/LIVE-COMPETITOR-RESEARCH.md)
- [Automatic prospect discovery](docs/AUTOMATED-PROSPECTING.md)
- [AI creative generation](docs/AI-CREATIVES.md)
- [Permission-based email marketing](docs/EMAIL-MARKETING.md)
- [Performance optimization](docs/PERFORMANCE-OPTIMIZATION.md)
- [Daily marketing planning](docs/DAILY-MARKETING.md)

## Required credentials / approvals to go further

| Capability | Needs |
|------------|-------|
| Live Claude generation | `ANTHROPIC_API_KEY`, set `SARAH_LLM_PROVIDER=anthropic` |
| LinkedIn publishing | LinkedIn OAuth2 app, authorized Page, `w_organization_social` scope |
| Facebook / Instagram | Meta app review, Page access token, publish permissions |
| Reddit | Reddit OAuth2 app, per-subreddit rule compliance |
| Website blog | CMS publishing integration/token |
| Search Console analytics | Google OAuth + Search Console API |
| Token encryption (prod) | `SARAH_TOKEN_KEY` = base64 of 32 random bytes |

---

## Acceptance criteria status

| # | Criterion | Status |
|---|-----------|--------|
| 1 | Owner can log into the dashboard | ✅ |
| 2 | Owner can chat with Sarah | ✅ |
| 3 | Sarah creates a campaign from a natural-language instruction | ✅ |
| 4 | Content Agent generates platform-specific drafts | ✅ |
| 5 | Compliance Agent evaluates drafts | ✅ |
| 6 | Sarah schedules compliant, authorized publications | ✅ |
| 7 | Sarah actually publishes to a configured Discord webhook | ✅ (live connector + tested HTTP path) |
| 8 | Disconnected/unsupported channels reported correctly | ✅ |
| 9 | Every action recorded in the database | ✅ (audit_log) |
| 10 | Owner can pause all autonomous activity immediately | ✅ (kill switch) |
| 11 | Failed jobs retried without duplicate posts | ✅ (idempotency + status guard) |
| 12 | Application starts through Docker Compose | ✅ (compose + Dockerfiles with healthchecks) |
| 13 | Backend and frontend tests pass | ✅ backend `mvn test`; frontend builds + typechecks |
