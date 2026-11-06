package com.oreostudios.sarah.audit.internal.domain; // Domain layer of the audit module.

import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Creation timestamp.
import java.util.UUID; // Identifier type.

/** Append-only audit record of a single platform action. */
@Entity // Managed entity.
@Table(name = "audit_log") // Backing table.
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Entry id.

    @Column(nullable = false)
    private String actor; // Who performed the action.

    @Column(nullable = false)
    private String action; // Action code.

    @Column(name = "entity_type")
    private String entityType; // Affected entity type (nullable).

    @Column(name = "entity_id")
    private String entityId; // Affected entity id (nullable).

    @Column
    private String detail; // Free-form detail (nullable).

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Recorded time.

    /** No-args constructor required by JPA. */
    protected AuditLog() {
    }

    /**
     * Creates an audit entry.
     *
     * @param actor      who performed the action
     * @param action     the action code
     * @param entityType the affected entity type, or null
     * @param entityId   the affected entity id, or null
     * @param detail     free-form detail, or null
     */
    public AuditLog(String actor, String action, String entityType, String entityId, String detail) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.detail = detail;
    }

    public UUID getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}
