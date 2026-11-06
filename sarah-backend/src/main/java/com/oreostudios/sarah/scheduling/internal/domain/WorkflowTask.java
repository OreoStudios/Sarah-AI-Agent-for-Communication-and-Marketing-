package com.oreostudios.sarah.scheduling.internal.domain; // Domain layer of the scheduling module.

import com.oreostudios.sarah.scheduling.api.TaskStatus; // Status enum (public).
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

/** A persisted unit of agent work (task queue + agent-to-agent handoff record). */
@Entity // Managed entity.
@Table(name = "workflow_task") // Backing table.
public class WorkflowTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Task id.

    @Column(name = "campaign_id")
    private UUID campaignId; // Owning campaign (nullable).

    @Column(nullable = false)
    private String type; // Task type code.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING; // Lifecycle status.

    @Column
    private String agent; // Responsible agent.

    @Column
    private String payload; // Optional payload.

    @Column(nullable = false)
    private int attempts = 0; // Attempts made.

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3; // Attempt ceiling.

    @Column(name = "last_error")
    private String lastError; // Last error detail.

    @Column(name = "scheduled_at")
    private Instant scheduledAt; // Optional scheduled time.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Creation time.

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now(); // Last update time.

    /** No-args constructor required by JPA. */
    protected WorkflowTask() {
    }

    /** Creates a PENDING task. */
    public WorkflowTask(UUID campaignId, String type, String agent, String payload) {
        this.campaignId = campaignId;
        this.type = type;
        this.agent = agent;
        this.payload = payload;
    }

    /** Moves to RUNNING and counts the attempt. */
    public void start() {
        this.status = TaskStatus.RUNNING;
        this.attempts++;
        this.updatedAt = Instant.now();
    }

    /** Moves to COMPLETED. */
    public void complete() {
        this.status = TaskStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    /** Moves to FAILED with an error. */
    public void fail(String error) {
        this.status = TaskStatus.FAILED;
        this.lastError = error;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCampaignId() { return campaignId; }
    public String getType() { return type; }
    public TaskStatus getStatus() { return status; }
    public String getAgent() { return agent; }
    public String getPayload() { return payload; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastError() { return lastError; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
