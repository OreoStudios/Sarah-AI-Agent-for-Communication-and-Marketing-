package com.oreostudios.sarah.brand.internal.domain; // Domain layer of the brand module.

import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Update timestamp.
import java.util.UUID; // Identifier type.

/** Configurable brand/company profile. Agents read this; it is never hardcoded in prompts. */
@Entity // Managed entity.
@Table(name = "brand_profile") // Backing table.
public class BrandProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Profile id.

    @Column(name = "company_name", nullable = false)
    private String companyName; // Company name.

    @Column(nullable = false)
    private String description; // Company description.

    /** Newline-separated services. */
    @Column(nullable = false)
    private String services; // Services list.

    @Column(nullable = false)
    private String tone; // Brand tone.

    /** CSV language codes, e.g. "en,fr". */
    @Column(nullable = false)
    private String languages; // Supported languages.

    private String website; // Marketing site URL (nullable).

    @Column(nullable = false)
    private boolean active = true; // Whether this is the active profile.

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now(); // Last update time.

    /** No-args constructor required by JPA. */
    protected BrandProfile() {
    }

    public UUID getId() { return id; }
    public String getCompanyName() { return companyName; }
    public String getDescription() { return description; }
    public String getServices() { return services; }
    public String getTone() { return tone; }
    public String getLanguages() { return languages; }
    public String getWebsite() { return website; }
    public boolean isActive() { return active; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Updates the mutable profile fields and bumps the update timestamp.
     */
    public void update(String companyName, String description, String services,
                       String tone, String languages, String website) {
        this.companyName = companyName;
        this.description = description;
        this.services = services;
        this.tone = tone;
        this.languages = languages;
        this.website = website;
        this.updatedAt = Instant.now();
    }
}
