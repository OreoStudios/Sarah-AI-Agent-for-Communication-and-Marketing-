package com.oreostudios.sarah.campaign.api; // Public API package of the campaign module.

import java.util.UUID; // Campaign id.

/**
 * Command to persist a content draft produced by an agent, with its compliance verdict.
 *
 * @param campaignId       owning campaign id
 * @param platform         platform code
 * @param language         content language code
 * @param title            optional title
 * @param body             content body
 * @param imageBrief        optional image/creative brief
 * @param complianceScore  compliance score (0-100)
 * @param complianceNotes  compliance advisory
 * @param approved         whether compliance approved the draft
 */
public record AddDraftCommand(
        UUID campaignId,
        String platform,
        String language,
        String title,
        String body,
        String imageBrief,
        int complianceScore,
        String complianceNotes,
        boolean approved
) {
}
