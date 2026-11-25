# Instagram and Reddit
Both connectors are off by default. Configure server environment and add channels in Settings. Secrets are stored by the existing encrypted-token mechanism. Use a separate named channel for each approved image or Reddit title; configuration is immutable in the current UI.

## Instagram
- Set SARAH_SOCIAL_INSTAGRAM_ENABLED=true and SARAH_INSTAGRAM_API_VERSION to a currently supported Graph API version (vNN.N).
- Add graph.facebook.com and the exact public media host to SARAH_OUTBOUND_HOSTS (preserve other approved hosts).
- Use a professional Instagram account connected to a Facebook Page, an authorized token with the relevant Instagram publishing and account permissions, and an owned or licensed public JPEG URL (no query credentials).
- Channel config: accountId and imageUrl. SARAH creates a single-photo container, checks FINISHED, then publishes. A not-ready container does not trigger a publication. No videos, carousels, stories or image generation in this release.
- Official docs: https://developers.facebook.com/docs/graph-api/reference/shadow-ig-user/media/ and https://developers.facebook.com/docs/graph-api/reference/shadow-ig-user/media_publish/

## Reddit
- Set SARAH_SOCIAL_REDDIT_ENABLED=true and SARAH_REDDIT_USER_AGENT to a descriptive app identifier with responsible Reddit account.
- Add oauth.reddit.com to SARAH_OUTBOUND_HOSTS.
- Obtain explicit Reddit API approval, including commercial-use permission if required, and a valid user OAuth token with identity and submit scopes. Channel config: subreddit (without r/) and approved title. The token must be replaced when expired; automated refresh is not implemented.
- Post only where permitted by the community; no cross-posting, bulk publishing or automated unsolicited messages. Posts include an automation/affiliation disclosure. Reddit application-level errors are treated as failures.
- Official docs: https://www.reddit.com/dev/api/ and https://support.reddithelp.com/hc/en-us/articles/42728983564564-Responsible-Builder-Policy

## Workflow
Generate an Instagram or Reddit campaign via chat. Review the approved draft and channel configuration in Campaigns and explicitly approve scheduling. The existing scheduler, global pause, rate limits and audit trail remain in effect. A draft can only be queued once through the reviewed-social endpoint; ambiguous remote outcomes are not automatically retried. Reconcile manually before any retry. A successful provider-assigned ID is required to report publication. No live publishing was performed in development.
