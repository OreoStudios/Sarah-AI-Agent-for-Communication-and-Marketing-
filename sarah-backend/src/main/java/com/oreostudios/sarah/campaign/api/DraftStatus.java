package com.oreostudios.sarah.campaign.api; // Public API package of the campaign module.

/** Lifecycle status of a content draft. */
public enum DraftStatus {
    DRAFT, // Generated, not yet evaluated.
    APPROVED, // Passed compliance.
    REJECTED // Failed compliance.
}
