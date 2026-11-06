package com.oreostudios.sarah.audit.api; // Public API package of the audit module.

import java.time.Instant; // Timestamp of the recorded action.

/**
 * Immutable projection of a recorded audit entry.
 *
 * @param actor      who performed the action (user email or "system")
 * @param action     the action code, e.g. {@code CAMPAIGN_CREATED}
 * @param entityType the affected entity type, or null
 * @param entityId   the affected entity id, or null
 * @param at         when the action was recorded
 */
public record AuditEntryView(String actor, String action, String entityType, String entityId, Instant at) {
}
