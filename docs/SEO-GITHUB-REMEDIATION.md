# SARAH — website audit to Astra-generated GitHub draft PR

## User workflow

1. Select your **company workspace** and set its correct website in Brand settings.
2. Go to **SEO & GEO** and run **Website audit** or **AI visibility**.
3. SARAH shows audit findings and an actionable plan, distinguishing website-code work from off-site work.
4. Connect the website's repository with a fine-grained GitHub access token scoped to **only that repository**.
5. Select the saved `crawl`, `action-plan`, `geo`, or `speed` report and select **Generate draft fix PR**.
6. SARAH uses Astra to propose **small, exact text replacements** in a limited set of website source files.
7. SARAH validates the edits against original GitHub source, creates a new branch, then opens a **draft PR**.
8. The company owner reviews the changes, confirms factual claims, runs CI, marks the draft ready, merges and **redeploys manually**. SARAH does not merge or deploy.

## GitHub setup

The repository URL is `https://github.com/OWNER/REPO`. Public/private repos require an authorized token to **write**.

Use a fine-grained GitHub token with repository permissions:
- **Contents: read and write** (fetch code, create branch and commit).
- **Pull requests: read and write** (create a draft PR).
- Metadata: read (provided by GitHub).

For production SaaS, a GitHub App installation flow with per-repository authorization is preferable to a stored PAT. The initial integration stores the token encrypted at rest with `SARAH_TOKEN_KEY`. **Set a stable base64-encoded 32-byte key in Coolify** before connecting; without it the integration rejects token persistence. Never paste the token into an AI chat.

## Required backend environment

```dotenv
OPENAI_API_KEY=<configured only in Coolify secrets>
SARAH_SEO_FIX_MODEL=gpt-6.2-astra
SARAH_TOKEN_KEY=<existing persistent 32-byte base64 key>
```

The separate SEO fix model may be changed to an API model available to your OpenAI project. The AI visibility sample still uses its own existing provider/model configuration. GitHub credentials **never** enter Astra's prompt.

## Security

- Every endpoint is under `/api/v1/seo/fixes`, requires workspace `OWNER`, and inherits company RLS scoping.
- All saved credentials and fix PRs are scoped to a verified company workspace by `workspace_id` and PostgreSQL FORCE RLS.
- Only the **api.github.com** endpoint is contacted for repositories, and the supplied repository URL is parsed with a strict allowlist.
- Source selection is bounded (up to 7 small text files) and excludes hidden, secret, test, binary and build output paths. No arbitrary shell commands or checkouts.
- The LLM is restricted to existing source files, at most 8 precise replacements across 5 changed files. Exact source match validation is required before a branch is created.
- Audit content and repository code are **untrusted prompt data**; they cannot authorize more permissions.
- Pull requests are created as **draft**. No merge or deployment operation is implemented.
- One PR is tracked per saved report and connected repository to reduce duplicate runs.

## What SARAH can and cannot fix

Suitable candidates: truthful page titles and descriptions, appropriate headings, relevant accessible content,
internal links, honest company/brand context, and verified structured data in supported templates.

Not fixed by a website PR: getting a third-party site to link to you, external reviews/brand mentions,
forced AI recommendation, guaranteeing search indexing, and operational Cloudflare, DNS or hosting problems.

Changes are generated as **proposals**. They need manual review and target-repo CI: this implementation
does not run untrusted website build scripts inside SARAH.

## Endpoints

- `GET /api/v1/seo/fixes/repository` — connection status, never token.
- `PUT /api/v1/seo/fixes/repository` — secure connection setup.
- `DELETE /api/v1/seo/fixes/repository` — revoke stored token.
- `POST /api/v1/seo/fixes/pull-request` — `{"auditId":123,"confirmed":true}` builds a draft PR.

## Deployment

Take a database backup before deploying Flyway V12. The migration introduces
`seo_github_connection` and `seo_fix_pr` with tenant RLS and no changes to existing marketing data.
Review GitHub Actions backend and frontend CI before merging.
