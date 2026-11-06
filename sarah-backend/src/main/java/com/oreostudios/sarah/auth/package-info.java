/**
 * Auth module: conversational, passwordless email-OTP authentication. Visitors meet Sarah,
 * introduce themselves, verify their email with a one-time code, and a server-side session is
 * created. Email validation, OTP generation/verification, session creation and authorization are
 * deterministic Java — the LLM is never involved and OTP codes are never passed to it.
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Auth",
        allowedDependencies = {"shared", "config", "audit :: api"} // Shared types, config/secrets, audit.
)
package com.oreostudios.sarah.auth; // Root package of the auth module.
