package com.oreostudios.sarah.campaign.api; // Public API package of the campaign module.

import java.time.Instant; // Creation timestamp.
import java.util.UUID; // Identifiers.

/**
 * Immutable projection of a content draft.
 *
 * @param id               draft id
 * @param campaignId       owning campaign id
 * @param platform         platform code
 * @param language         content language code
 * @param title            optional title
 * @param body             content body
 * @param imageBrief        optional image/creative brief
 * @param status           draft lifecycle status
 * @param complianceScore  compliance score (0-100), or null
 * @param complianceNotes  compliance advisory, or null
 * @param createdAt        creation time
 */
public record DraftView(
        UUID id,
        UUID campaignId,
        String platform,
        String language,
        String title,
        String body,
        String imageBrief,
        DraftStatus status,
        Integer complianceScore,
        String complianceNotes,
        Instant createdAt
) {
}
