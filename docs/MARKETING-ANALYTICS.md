# Marketing delivery analytics

`GET /api/v1/dashboard/analytics?days=30` (OWNER). Cohort selected by scheduled date; status reflects current persisted outcomes. `deliverySuccessRate` is published/(published+failed), null if no completed attempts. These are delivery records, not impressions, conversions or ROI.
