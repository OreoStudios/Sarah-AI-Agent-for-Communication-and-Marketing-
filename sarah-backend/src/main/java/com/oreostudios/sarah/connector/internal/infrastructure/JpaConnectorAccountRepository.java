package com.oreostudios.sarah.connector.internal.infrastructure; // Infrastructure layer of the connector module.

import com.oreostudios.sarah.connector.api.ConnectorStatus; // Status filter.
import com.oreostudios.sarah.connector.internal.domain.ConnectorAccount; // Managed entity.
import com.oreostudios.sarah.shared.Platform; // Platform filter.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.Optional; // Optional lookup.
import java.util.UUID; // Primary-key type.

/** Spring Data repository for {@link ConnectorAccount}. */
public interface JpaConnectorAccountRepository extends JpaRepository<ConnectorAccount, UUID> {
    /** @return the first channel of the given platform in the given status */
    Optional<ConnectorAccount> findFirstByTypeAndStatus(Platform type, ConnectorStatus status);
}
