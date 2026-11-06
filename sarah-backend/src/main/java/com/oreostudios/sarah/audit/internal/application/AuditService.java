package com.oreostudios.sarah.audit.internal.application; // Application layer of the audit module.

import com.oreostudios.sarah.audit.api.AuditEntryView; // Outbound DTO.
import com.oreostudios.sarah.audit.api.AuditFacade; // Implemented public port.
import com.oreostudios.sarah.audit.internal.domain.AuditLog; // Entity.
import com.oreostudios.sarah.audit.internal.infrastructure.JpaAuditLogRepository; // Persistence gateway.
import org.springframework.data.domain.PageRequest; // Pagination request.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Propagation; // Transaction propagation.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

import java.util.List; // Return type.

/** Writes and reads audit records. Writes join the caller's transaction. */
@Service // Spring-managed bean implementing the audit port.
public class AuditService implements AuditFacade {

    private final JpaAuditLogRepository repository; // Persistence gateway.

    /** @param repository audit-log repository */
    AuditService(JpaAuditLogRepository repository) {
        this.repository = repository; // Store the repository.
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED) // Join the caller's transaction.
    public void record(String actor, String action, String entityType, String entityId, String detail) {
        repository.save(new AuditLog(actor, action, entityType, entityId, detail)); // Persist the entry.
    }

    @Override
    @Transactional(readOnly = true) // Read-only query.
    public List<AuditEntryView> recent(int page, int size) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 200))).stream() // Cap page size.
                .map(a -> new AuditEntryView(a.getActor(), a.getAction(), a.getEntityType(), a.getEntityId(), a.getCreatedAt())) // Map to DTO.
                .toList();
    }
}
