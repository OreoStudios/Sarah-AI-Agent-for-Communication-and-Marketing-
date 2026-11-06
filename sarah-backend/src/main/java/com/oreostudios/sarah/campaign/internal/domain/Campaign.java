package com.oreostudios.sarah.campaign.internal.domain; // Domain layer of the campaign module.

import com.oreostudios.sarah.campaign.api.CampaignStatus; // Workflow states (public enum).
import com.oreostudios.sarah.shared.CsvListConverter; // CSV list persistence.
import com.oreostudios.sarah.shared.PolicyViolationException; // Illegal-transition signal.
import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Convert; // Attribute converter binding.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.EnumType; // Enum persistence selector.
import jakarta.persistence.Enumerated; // Enum persistence declaration.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Schedule/timestamps.
import java.util.List; // Platform codes.
import java.util.UUID; // Identifier type.

/** A marketing campaign and its workflow state (aggregate root). */
@Entity // Managed entity.
@Table(name = "campaign") // Backing table.
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Campaign id.

    @Column(nullable = false, unique = true)
    private String name; // Unique name (prevents duplicate campaigns).

    @Column(nullable = false)
    private String objective; // Marketing objective / brief.

    @Column(name = "target_audience", nullable = false)
    private String targetAudience; // Intended audience.

    @Convert(converter = CsvListConverter.class)
    @Column(nullable = false)
    private List<String> platforms; // Target platform codes (CSV column).

    @Column(nullable = false)
    private String language = "en"; // Content language code.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.CREATED; // Workflow state.

    @Column(name = "budget_cents", nullable = false)
    private long budgetCents = 0; // Budget in cents.

    @Column(name = "start_at")
    private Instant startAt; // Optional start time.

    @Column(name = "end_at")
    private Instant endAt; // Optional end time.

    @Column(name = "created_by", nullable = false)
    private String createdBy; // Creating actor.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Creation time.

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now(); // Last update time.

    /** No-args constructor required by JPA. */
    protected Campaign() {
    }

    /** Creates a campaign in the {@link CampaignStatus#CREATED} state. */
    public Campaign(String name, String objective, String targetAudience, List<String> platforms,
                    String language, long budgetCents, Instant startAt, Instant endAt, String createdBy) {
        this.name = name;
        this.objective = objective;
        this.targetAudience = targetAudience;
        this.platforms = platforms;
        this.language = language;
        this.budgetCents = budgetCents;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdBy = createdBy;
        this.status = CampaignStatus.CREATED;
    }

    /**
     * Deterministic state-machine transition. Rejects illegal transitions.
     *
     * @param next the target state
     * @throws PolicyViolationException if the transition is not allowed
     */
    public void transitionTo(CampaignStatus next) {
        if (this.status == next) { // No-op on same state.
            return;
        }
        if (!this.status.canTransitionTo(next)) { // Reject illegal transitions.
            throw new PolicyViolationException("Illegal campaign transition %s → %s".formatted(this.status, next));
        }
        this.status = next; // Apply.
        this.updatedAt = Instant.now(); // Bump timestamp.
    }

    /** Edits the mutable fields. */
    public void edit(String objective, String targetAudience, List<String> platforms,
                     String language, long budgetCents, Instant startAt, Instant endAt) {
        this.objective = objective;
        this.targetAudience = targetAudience;
        this.platforms = platforms;
        this.language = language;
        this.budgetCents = budgetCents;
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getObjective() { return objective; }
    public String getTargetAudience() { return targetAudience; }
    public List<String> getPlatforms() { return platforms; }
    public String getLanguage() { return language; }
    public CampaignStatus getStatus() { return status; }
    public long getBudgetCents() { return budgetCents; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
