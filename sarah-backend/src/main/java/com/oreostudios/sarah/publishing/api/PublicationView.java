package com.oreostudios.sarah.publishing.api; // Public API package of the publishing module.

import com.oreostudios.sarah.shared.Platform; // Platform enum (shared).

import java.time.Instant; // Timestamps.
import java.util.UUID; // Identifiers.

/**
 * Immutable projection of a publication (content calendar + history).
 *
 * @param id                  publication id
 * @param campaignId          owning campaign id (nullable)
 * @param draftId             source draft id (nullable)
 * @param connectorAccountId  target channel id
 * @param platform            target platform
 * @param content             the content
 * @param scheduledAt         scheduled time
 * @param status              lifecycle status
 * @param attempts            attempts made
 * @param maxAttempts         attempt ceiling
 * @param externalId          platform id once published (nullable)
 * @param externalUrl         permalink once published (nullable)
 * @param lastError           last error, if any
 * @param publishedAt         publish time (nullable)
 */
public record PublicationView(
        UUID id,
        UUID campaignId,
        UUID draftId,
        UUID connectorAccountId,
        Platform platform,
        String content,
        Instant scheduledAt,
        PublicationStatus status,
        int attempts,
        int maxAttempts,
        String externalId,
        String externalUrl,
        String lastError,
        Instant publishedAt
) {
}
