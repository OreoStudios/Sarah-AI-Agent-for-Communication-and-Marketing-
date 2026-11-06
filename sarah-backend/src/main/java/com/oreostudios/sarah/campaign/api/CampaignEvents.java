package com.oreostudios.sarah.campaign.api; // Public API package of the campaign module.

import java.util.UUID; // Campaign id.

/** Domain events published by the campaign module (consumed via Spring application events). */
public final class CampaignEvents {

    private CampaignEvents() {
    }

    /**
     * Published when a campaign is created.
     *
     * @param campaignId the new campaign id
     * @param name       the campaign name
     * @param createdBy  the actor who created it
     */
    public record CampaignCreated(UUID campaignId, String name, String createdBy) {
    }

    /**
     * Published when a campaign changes workflow state.
     *
     * @param campaignId the campaign id
     * @param from       the previous state
     * @param to         the new state
     */
    public record CampaignStatusChanged(UUID campaignId, CampaignStatus from, CampaignStatus to) {
    }
}
