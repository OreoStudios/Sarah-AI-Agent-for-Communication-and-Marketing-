package com.oreostudios.sarah.security.internal.domain; // Domain layer of the security module.

/** Role-based access control roles. OWNER has full control of autonomous activity. */
public enum Role {
    OWNER, // Full control, including the kill switch.
    OPERATOR, // Can manage campaigns/content/channels.
    VIEWER // Read-only access.
}
