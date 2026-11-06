package com.oreostudios.sarah.audit.internal.infrastructure; // Infrastructure layer of the audit module.

import com.oreostudios.sarah.audit.internal.domain.AuditLog; // Managed entity.
import org.springframework.data.domain.Page; // Paged result.
import org.springframework.data.domain.Pageable; // Pagination request.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.UUID; // Primary-key type.

/** Spring Data repository for {@link AuditLog}. */
public interface JpaAuditLogRepository extends JpaRepository<AuditLog, UUID> {
    /** @return audit entries newest first */
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
