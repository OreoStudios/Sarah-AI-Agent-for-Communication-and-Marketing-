# SEO & GEO intelligence (November 2026)

Owner-only dashboard /seo and /api/v1/seo endpoints. Defaults are opt-in for scheduled traffic; paid provider calls are manual. Each report is persisted in PostgreSQL; the daily crawler defaults to 07:00 Europe/Paris and prunes snapshots older than 90 days when enabled. Run one instance only for scheduled jobs; the concurrency lock is local.

Crawler: fixed HTTPS origin, no credentials, query strings or automatic redirect following, bounded pages/time/body, conservative robots checks, XML sitemap traversal, title/description/headings/ALT/canonical/noindex/JSON-LD presence, exact duplicate text/title, internal HTTP errors and AI robots root access. Configure deployment egress to deny private/metadata networks. No JS rendering; no promise of exhaustive discovery; robots matching is a subset of RFC 9309. JSON-LD presence is not schema validation. PageSpeed is on demand for mobile/desktop and provides field metrics if available, plus lab metrics. Do not interpret TBT as INP.

DataForSEO v3: summary, first 100 backlink rows, Google ranked keywords and keyword suggestions (France/French). Domain input allows competitor comparisons. Provider result data is stored as returned. No exhaustive link index or automatic lost/new comparison yet. GEO uses neutral queries and native search tools, records answer/model/citation URLs and observed mentions. Citations may be proxied or omitted; no consumer UI rank equivalence. All external reports require provider credentials and may cost money. No automatic publishing or IndexNow submission.

Official references:
- https://developers.openai.com/api/docs/models and https://developers.openai.com/api/docs/guides/tools-web-search
- https://platform.claude.com/docs/en/models/overview and https://platform.claude.com/docs/en/agents-and-tools/tool-use/web-search-tool
- https://aistudio.google.com/docs/models and https://aistudio.google.com/docs/google-search
- https://docs.dataforseo.com/v3/backlinks/backlinks/live/
- https://docs.dataforseo.com/v3/dataforseo_labs/google/keyword_suggestions/live/
- https://developers.google.com/speed/docs/insights/rest
- https://jsoup.org/news/release-1.24.1

Configuration: see .env.example. Model overrides are intentional; verify model entitlement on each account. Existing general OpenAI generation now uses Responses API, not legacy chat completions. Existing Claude chat retains Spring AI integration; GEO uses native Messages API.

Remaining scope: full backlink pagination and diff, GSC URL inspection and pagination, Bing Webmaster and IndexNow, automated multi-provider GEO/keyword schedules with budget enforcement, AI strategic synthesis, rendered crawling and richer dashboard. Live credentials and full production testing required.
