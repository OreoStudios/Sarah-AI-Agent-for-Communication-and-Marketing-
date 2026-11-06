package com.oreostudios.sarah.autonomy.internal.infrastructure; // Infrastructure layer of the autonomy module.

import com.oreostudios.sarah.autonomy.internal.domain.AutonomySettings; // Managed entity.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.Optional; // Optional lookup.
import java.util.UUID; // Primary-key type.

/** Spring Data repository for {@link AutonomySettings}. */
public interface JpaAutonomySettingsRepository extends JpaRepository<AutonomySettings, UUID> {
    /** @return the single active settings row (most recently updated) */
    Optional<AutonomySettings> findFirstByOrderByUpdatedAtDesc();
}
