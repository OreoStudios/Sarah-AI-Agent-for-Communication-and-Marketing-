package com.oreostudios.sarah.publishing.internal.domain; // Domain layer of the publishing module.

import com.oreostudios.sarah.publishing.api.PublicationStatus; // Status enum (public).
import com.oreostudios.sarah.shared.Platform; // Platform enum (shared).
import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.EnumType; // Enum persistence selector.
import jakarta.persistence.Enumerated; // Enum persistence declaration.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Timestamps.
import java.util.UUID; // Identifier type.

/** A scheduled/published piece of content for one channel (aggregate root). */
@Entity // Managed entity.
@Table(name = "publication") // Backing table.
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Publication id.

    @Column(name = "campaign_id")
    private UUID campaignId; // Owning campaign (nullable).

    @Column(name = "draft_id")
    private UUID draftId; // Source draft (nullable).

    @Column(name = "connector_account_id", nullable = false)
    private UUID connectorAccountId; // Target channel.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform platform; // Target platform.

    @Column(nullable = false)
    private String content; // Content body.

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt; // When to publish.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationStatus status = PublicationStatus.SCHEDULED; // Lifecycle status.

    @Column(nullable = false)
    private int attempts = 0; // Attempts made.

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3; // Attempt ceiling.

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey; // Unique key (prevents duplicate posts).

    @Column(name = "external_id")
    private String externalId; // Platform id once published.

    @Column(name = "external_url")
    private String externalUrl; // Permalink once published.

    @Column(name = "last_error")
    private String lastError; // Last error detail.

    @Column(name = "published_at")
    private Instant publishedAt; // Publish time.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Creation time.

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now(); // Last update time.

    /** No-args constructor required by JPA. */
    protected Publication() {
    }

    /** Creates a SCHEDULED publication. */
    public Publication(UUID campaignId, UUID draftId, UUID connectorAccountId, Platform platform,
                       String content, Instant scheduledAt, int maxAttempts, String idempotencyKey) {
        this.campaignId = campaignId;
        this.draftId = draftId;
        this.connectorAccountId = connectorAccountId;
        this.platform = platform;
        this.content = content;
        this.scheduledAt = scheduledAt;
        this.maxAttempts = maxAttempts;
        this.idempotencyKey = idempotencyKey;
    }

    /** Claims the publication for an attempt. */
    public void markPublishing() {
        this.status = PublicationStatus.PUBLISHING;
        this.attempts += 1;
        this.updatedAt = Instant.now();
    }

    /** Marks a successful publish with its external identifiers. */
    public void markPublished(String externalId, String externalUrl) {
        this.status = PublicationStatus.PUBLISHED;
        this.externalId = externalId;
        this.externalUrl = externalUrl;
        this.publishedAt = Instant.now();
        this.lastError = null;
        this.updatedAt = Instant.now();
    }

    /** Transient failure: reschedule for a later retry while attempts remain. */
    public void markRetry(String error, Instant nextAttemptAt) {
        this.status = PublicationStatus.SCHEDULED;
        this.lastError = error;
        this.scheduledAt = nextAttemptAt;
        this.updatedAt = Instant.now();
    }

    /** Permanent failure. */
    public void markFailed(String error) {
        this.status = PublicationStatus.FAILED;
        this.lastError = error;
        this.updatedAt = Instant.now();
    }

    /** Cancellation by the owner. */
    public void markCancelled() {
        this.status = PublicationStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /** @return whether another attempt is permitted */
    public boolean attemptsRemaining() {
        return attempts < maxAttempts;
    }

    public UUID getId() { return id; }
    public UUID getCampaignId() { return campaignId; }
    public UUID getDraftId() { return draftId; }
    public UUID getConnectorAccountId() { return connectorAccountId; }
    public Platform getPlatform() { return platform; }
    public String getContent() { return content; }
    public Instant getScheduledAt() { return scheduledAt; }
    public PublicationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getExternalId() { return externalId; }
    public String getExternalUrl() { return externalUrl; }
    public String getLastError() { return lastError; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
