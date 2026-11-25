# Company-owned workspaces (SARAH SaaS)

## Overview
SARAH is a shared application with **separate, privately owned company workspaces**.
Email verification establishes a user identity. The user can create a company workspace,
becomes **OWNER** of that company (not of the SARAH platform), and may create/join more.
The current workspace ID is stored in the authenticated server-side session, never trusted
from a client-supplied header or URL for data access. Switching is allowed only for members.

Roles inside each workspace:
- OWNER: company-level control, including automation and team management
- ADMIN / MEMBER: operator privileges for campaigns and channels (no owner-only actions)
- VIEWER: read-only private API access

`SARAH_ADMIN_EMAIL` remains a bootstrap **platform** credential and does not grant
all registered customers global company ownership. It need not match each signup.

## Database isolation
V11 adds `workspace` and `workspace_membership` and a `workspace_id` to all
marketing/business tables. Old rows are assigned to a fixed unclaimed **legacy quarantine**
workspace (`00000000-0000-0000-0000-000000000001`), with no membership.

For every `/api/v1/**` request:
1. Require an authenticated and verified principal.
2. Resolve workspace from server session (or auto-select if exactly one).
3. Query workspace membership based on the authenticated email.
4. Run within a single transaction using `SET LOCAL ROLE sarah_workspace_runtime`
   and `set_config('sarah.workspace_id', workspace-id, true)`.
5. PostgreSQL `FORCE ROW LEVEL SECURITY` policies restrict all tenant-owned tables
   and prevent access without the current workspace setting.
6. Additional composite FKs forbid referencing another tenant's campaign or connector.

**Assumption:** Flyway uses the PostgreSQL schema owner/superuser configured by the
existing deployment to create the non-login restricted runtime role and to set that
role for tenant-bound requests. Do not switch to an unprivileged DB user without
separately providing the needed role membership and Flyway migration credentials.

## Rollout: production backup required
1. Create a verified **PostgreSQL backup** and restore it into a staging DB.
2. Run Flyway V11 against the staging copy. Check constraints and pre-existing data.
3. Run GitHub CI, including PostgreSQL Testcontainers isolation tests.
4. Verify guest OTP login, company creation, two independent companies, and team roles.
5. Verify `GET /actuator/health` and public HTTPS API routing.
6. Only then merge and redeploy SARAH via Coolify.

The legacy quarantine **does not automatically transfer existing company data** to
your email address. A controlled admin data migration must explicitly assign ownership
to a verified company owner after validating the original tenant; never auto-claim it.

## API
- `GET /api/workspaces` — your companies and active selection.
- `POST /api/workspaces` — create company (creator becomes OWNER).
- `POST /api/workspaces/{id}/select` — switch to one of your member companies.
- `GET /api/workspaces/{id}/members` — company OWNER only.
- `POST /api/workspaces/{id}/members` — owner adds ADMIN/MEMBER/VIEWER.
- `DELETE /api/workspaces/{id}/members/{email}` — remove non-owner membership.

The member-add action records authorized membership; it does not send invitations.
Recipients must verify ownership of the exact email address through the usual OTP flow.

## Existing automation
Publishing jobs execute once per workspace within a tenant-restricted transaction.
The old global daily SEO, prospecting, marketing-plan and strategy-optimization scheduled
paths are disabled by default (the `SARAH_WORKSPACE_LEGACY_SCHEDULERS` toggle should
remain `false` in production) until reworked for per-company inputs/budgets. Do not
enable the legacy flag on a multi-company production server.

## Security boundaries & remaining work
- RLS is required for new customer data; do not remove the `SET LOCAL ROLE` or
  `workspace_id` setting from transaction handling.
- Do not expose the PostgreSQL owner credentials to clients.
- Verify tenant scoping for any newly added tables, external connectors, workers or APIs.
- Billing, plan quotas and automatic email invitations are separate future features.
- Take care with SaaS-wide provider API credentials, which remain platform-managed.
