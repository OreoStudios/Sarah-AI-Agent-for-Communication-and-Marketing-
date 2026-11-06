package com.oreostudios.sarah.campaign.internal.application; // Application layer of the campaign module.

import com.oreostudios.sarah.campaign.api.CampaignView; // Campaign DTO.
import com.oreostudios.sarah.campaign.api.DraftView; // Draft DTO.
import com.oreostudios.sarah.campaign.internal.domain.Campaign; // Campaign entity.
import com.oreostudios.sarah.campaign.internal.domain.ContentDraft; // Draft entity.

/** Stateless helper mapping campaign entities to their public DTOs. */
class CampaignMapper {

    /** @return the campaign projected to a DTO */
    static CampaignView toView(Campaign c) {
        return new CampaignView(c.getId(), c.getName(), c.getObjective(), c.getTargetAudience(),
                c.getPlatforms(), c.getLanguage(), c.getStatus(), c.getBudgetCents(),
                c.getStartAt(), c.getEndAt(), c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt());
    }

    /** @return the draft projected to a DTO */
    static DraftView toView(ContentDraft d) {
        return new DraftView(d.getId(), d.getCampaignId(), d.getPlatform(), d.getLanguage(),
                d.getTitle(), d.getBody(), d.getImageBrief(), d.getStatus(),
                d.getComplianceScore(), d.getComplianceNotes(), d.getCreatedAt());
    }
}
