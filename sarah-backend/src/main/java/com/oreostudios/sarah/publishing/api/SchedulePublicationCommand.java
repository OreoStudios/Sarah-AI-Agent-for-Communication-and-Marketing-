package com.oreostudios.sarah.publishing.api; // Public API package of the publishing module.

import com.oreostudios.sarah.shared.Platform; // Platform enum (shared).

import java.time.Instant; // Scheduled time.
import java.util.UUID; // Identifiers.

/**
 * Command to schedule a publication. Idempotent on {@code idempotencyKey}.
 *
 * @param campaignId          owning campaign id (nullable)
 * @param draftId             source draft id (nullable)
 * @param connectorAccountId  target channel id
 * @param platform            target platform
 * @param content             the content to publish
 * @param scheduledAt         when to publish
 * @param maxAttempts         maximum delivery attempts
 * @param idempotencyKey      unique key so retries never double-post
 */
public record SchedulePublicationCommand(
        UUID campaignId,
        UUID draftId,
        UUID connectorAccountId,
        Platform platform,
        String content,
        Instant scheduledAt,
        int maxAttempts,
        String idempotencyKey
) {
}
