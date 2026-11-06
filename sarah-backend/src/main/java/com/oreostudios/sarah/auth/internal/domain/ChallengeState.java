package com.oreostudios.sarah.auth.internal.domain; // Domain layer of the auth module.

/** State of a single verification challenge. */
public enum ChallengeState {
    SENT,     // Code issued, awaiting verification.
    CONSUMED, // Successfully verified (single-use spent).
    LOCKED    // Too many failed attempts.
}
