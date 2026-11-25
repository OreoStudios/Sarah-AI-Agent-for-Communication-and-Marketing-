# Search Console

Set `SARAH_SEARCH_CONSOLE_ENABLED=true`, `SARAH_GSC_PROPERTY`, `SARAH_GSC_CLIENT_ID`, `SARAH_GSC_CLIENT_SECRET`, `SARAH_GSC_REFRESH_TOKEN` on the backend. Obtain refresh token for the webmasters.readonly scope and grant access only to your verified property. GET `/api/v1/dashboard/search-console?days=28&dimension=query` as OWNER. Returns top 100 final rows, not complete search traffic. No secrets in logs or model prompts. API docs: https://developers.google.com/webmaster-tools/v1/searchanalytics/query
