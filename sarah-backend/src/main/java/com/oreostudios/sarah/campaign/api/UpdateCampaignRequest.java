package com.oreostudios.sarah.campaign.api; // Public API package of the campaign module.

import jakarta.validation.constraints.NotBlank; // Non-blank validation.
import jakarta.validation.constraints.NotEmpty; // Non-empty validation.
import jakarta.validation.constraints.PositiveOrZero; // Budget validation.
import jakarta.validation.constraints.Size; // Length validation.

import java.time.Instant; // Schedule times.
import java.util.List; // Platform codes.

/**
 * Request to edit a campaign's mutable fields (the name is immutable).
 *
 * @param objective      marketing objective / brief
 * @param targetAudience intended audience
 * @param platforms      target platform codes
 * @param language       content language code
 * @param budgetCents    budget in cents
 * @param startAt        optional start time
 * @param endAt          optional end time
 */
public record UpdateCampaignRequest(
        @NotBlank String objective,
        @NotBlank @Size(max = 500) String targetAudience,
        @NotEmpty List<String> platforms,
        @Size(max = 8) String language,
        @PositiveOrZero long budgetCents,
        Instant startAt,
        Instant endAt
) {
}
