package com.oreostudios.sarah.scheduling.api; // Public API package of the scheduling module.

import java.time.Instant; // Update timestamp.
import java.util.UUID; // Identifiers.

/**
 * Immutable projection of a workflow task (agent-activity timeline).
 *
 * @param id         task id
 * @param campaignId owning campaign id (nullable)
 * @param type       task type code
 * @param status     lifecycle status
 * @param agent      the agent responsible
 * @param attempts   attempts made
 * @param lastError  last error, if any
 * @param updatedAt  last update time
 */
public record TaskView(UUID id, UUID campaignId, String type, TaskStatus status,
                       String agent, int attempts, String lastError, Instant updatedAt) {
}
