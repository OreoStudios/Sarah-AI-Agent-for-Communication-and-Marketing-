package com.oreostudios.sarah.brand.internal.infrastructure; // Infrastructure layer of the brand module.

import com.oreostudios.sarah.brand.internal.domain.BrandProfile; // Managed entity.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.Optional; // Optional lookup.
import java.util.UUID; // Primary-key type.

/** Spring Data repository for {@link BrandProfile}. */
public interface JpaBrandProfileRepository extends JpaRepository<BrandProfile, UUID> {
    /** @return the most recently updated active profile, if any */
    Optional<BrandProfile> findFirstByActiveTrueOrderByUpdatedAtDesc();
}
