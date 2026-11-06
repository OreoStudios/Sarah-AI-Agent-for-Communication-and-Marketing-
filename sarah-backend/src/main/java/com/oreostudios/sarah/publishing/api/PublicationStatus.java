package com.oreostudios.sarah.publishing.api; // Public API package of the publishing module.

/** Lifecycle of a scheduled publication. */
public enum PublicationStatus {
    SCHEDULED, // Waiting for its scheduled time / a retry.
    PUBLISHING, // In-flight (claimed for an attempt).
    PUBLISHED, // Successfully published.
    FAILED, // Permanently failed / attempts exhausted.
    CANCELLED // Cancelled by the owner.
}
