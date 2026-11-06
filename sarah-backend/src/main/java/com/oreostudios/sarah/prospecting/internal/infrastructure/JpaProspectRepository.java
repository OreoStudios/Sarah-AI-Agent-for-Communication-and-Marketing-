package com.oreostudios.sarah.prospecting.internal.infrastructure; // Infrastructure layer of the prospecting module.

import com.oreostudios.sarah.prospecting.internal.domain.Prospect; // Managed entity.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.List; // Return type.
import java.util.UUID; // Primary-key type.

/** Spring Data repository for {@link Prospect}. */
public interface JpaProspectRepository extends JpaRepository<Prospect, UUID> {
    /** @return prospects ordered by descending lead score */
    List<Prospect> findAllByOrderByLeadScoreDesc();
}
