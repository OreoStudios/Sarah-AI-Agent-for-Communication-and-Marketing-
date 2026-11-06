package com.oreostudios.sarah.autonomy.internal.domain; // Domain layer of the autonomy module.

import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Update timestamp.
import java.util.UUID; // Identifier type.

/** Persisted autonomy controls. A single active row governs the whole platform. */
@Entity // Managed entity.
@Table(name = "autonomy_settings") // Backing table.
public class AutonomySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Settings id.

    /** Global kill switch: when false, no autonomous publishing runs. */
    @Column(nullable = false)
    private boolean enabled = true; // Kill switch.

    @Column(name = "daily_post_limit", nullable = false)
    private int dailyPostLimit = 10; // Daily publication cap.

    @Column(name = "per_platform_cooldown_minutes", nullable = false)
    private int perPlatformCooldownMinutes = 30; // Cooldown between posts per platform.

    @Column(name = "max_llm_calls_per_run", nullable = false)
    private int maxLlmCallsPerRun = 25; // LLM-call budget per run.

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now(); // Last update time.

    /** No-args constructor required by JPA. */
    protected AutonomySettings() {
    }

    /** Toggles the kill switch. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    /** Updates the operating limits. */
    public void updateLimits(int dailyPostLimit, int cooldownMinutes, int maxLlmCalls) {
        this.dailyPostLimit = dailyPostLimit;
        this.perPlatformCooldownMinutes = cooldownMinutes;
        this.maxLlmCallsPerRun = maxLlmCalls;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public int getDailyPostLimit() { return dailyPostLimit; }
    public int getPerPlatformCooldownMinutes() { return perPlatformCooldownMinutes; }
    public int getMaxLlmCallsPerRun() { return maxLlmCallsPerRun; }
    public Instant getUpdatedAt() { return updatedAt; }
}
