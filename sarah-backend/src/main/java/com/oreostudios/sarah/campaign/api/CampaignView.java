package com.oreostudios.sarah.campaign.api; // Public API package of the campaign module.

import java.time.Instant; // Schedule/timestamps.
import java.util.List; // Platform codes.
import java.util.UUID; // Identifier.

/**
 * Immutable projection of a campaign.
 *
 * @param id             campaign id
 * @param name           unique name
 * @param objective      marketing objective / brief
 * @param targetAudience intended audience
 * @param platforms      target platform codes
 * @param language       content language code
 * @param status         workflow state
 * @param budgetCents    budget in cents
 * @param startAt        optional start time
 * @param endAt          optional end time
 * @param createdBy      actor who created it
 * @param createdAt      creation time
 * @param updatedAt      last update time
 */
public record CampaignView(
        UUID id,
        String name,
        String objective,
        String targetAudience,
        List<String> platforms,
        String language,
        CampaignStatus status,
        long budgetCents,
        Instant startAt,
        Instant endAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
