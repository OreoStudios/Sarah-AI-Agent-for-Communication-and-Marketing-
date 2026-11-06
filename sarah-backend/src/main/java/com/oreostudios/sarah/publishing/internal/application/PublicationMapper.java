package com.oreostudios.sarah.publishing.internal.application; // Application layer of the publishing module.

import com.oreostudios.sarah.publishing.api.PublicationView; // Public DTO.
import com.oreostudios.sarah.publishing.internal.domain.Publication; // Entity.

/** Stateless helper mapping a {@link Publication} to its {@link PublicationView}. */
class PublicationMapper {

    /** @return the publication projected to a DTO */
    static PublicationView toView(Publication p) {
        return new PublicationView(p.getId(), p.getCampaignId(), p.getDraftId(), p.getConnectorAccountId(),
                p.getPlatform(), p.getContent(), p.getScheduledAt(), p.getStatus(), p.getAttempts(),
                p.getMaxAttempts(), p.getExternalId(), p.getExternalUrl(), p.getLastError(), p.getPublishedAt());
    }
}
