package com.oreostudios.sarah.auth.internal.domain; // Domain layer of the auth module.

/** The conversational authentication state, surfaced to the frontend as a string. */
public enum AuthState {
    GUEST,         // No challenge, not authenticated.
    EMAIL_PENDING, // We asked for an email / are expecting one.
    CODE_SENT,     // A code has been issued and emailed; awaiting entry.
    VERIFYING,     // A code was submitted and is being checked.
    AUTHENTICATED, // Verified; a session exists.
    EXPIRED,       // The active challenge expired.
    LOCKED         // Too many attempts; the challenge is locked.
}
