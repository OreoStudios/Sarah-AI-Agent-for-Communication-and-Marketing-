package com.oreostudios.sarah.campaign.internal.infrastructure; // Infrastructure layer of the campaign module.

import com.oreostudios.sarah.campaign.internal.domain.Campaign; // Managed entity.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.List; // Return type.
import java.util.UUID; // Primary-key type.

/** Spring Data repository for {@link Campaign}. */
public interface JpaCampaignRepository extends JpaRepository<Campaign, UUID> {
    /** @return whether a campaign with this name exists (case-insensitive) */
    boolean existsByNameIgnoreCase(String name);

    /** @return all campaigns, newest first */
    List<Campaign> findAllByOrderByCreatedAtDesc();
}
