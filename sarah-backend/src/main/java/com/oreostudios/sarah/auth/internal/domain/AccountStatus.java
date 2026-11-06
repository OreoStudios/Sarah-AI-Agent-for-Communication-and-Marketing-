package com.oreostudios.sarah.auth.internal.domain; // Domain layer of the auth module.

/** Lifecycle of a user account. */
public enum AccountStatus {
    PENDING, // Registered but never verified.
    ACTIVE,  // Verified at least once.
    LOCKED   // Administratively locked.
}
