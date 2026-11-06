package com.oreostudios.sarah.campaign.internal.infrastructure; // Infrastructure layer of the campaign module.

import com.oreostudios.sarah.campaign.internal.domain.ContentDraft; // Managed entity.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.List; // Return type.
import java.util.UUID; // Primary-key / campaign id type.

/** Spring Data repository for {@link ContentDraft}. */
public interface JpaContentDraftRepository extends JpaRepository<ContentDraft, UUID> {
    /** @return the campaign's drafts in creation order */
    List<ContentDraft> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId);
}
