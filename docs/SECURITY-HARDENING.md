# Owner-only workspace

OTP proves control of an email, not authorization. The configured SARAH_ADMIN_EMAIL is the only OTP principal granted OWNER. Other verified accounts receive VIEWER and are denied workspace APIs. Existing stored OWNER roles are reset; the configured owner is resolved at login. Invalidate pre-existing sessions by restarting all application instances before exposing the service. Explicit multi-user tenancy and invitations are future work. Configure real SMTP, secure cookies, unique HMAC and encryption secrets before production.
