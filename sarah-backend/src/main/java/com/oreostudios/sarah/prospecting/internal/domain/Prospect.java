package com.oreostudios.sarah.prospecting.internal.domain; // Domain layer of the prospecting module.

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

/** A B2B prospect in the CRM. Respects suppression/opt-out (GDPR / FR e-marketing rules). */
@Entity // Managed entity.
@Table(name = "prospect") // Backing table.
public class Prospect {

    /** Contact lifecycle status. */
    public enum ContactStatus { NEW, CONTACTED, REPLIED, QUALIFIED, WON, LOST }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Prospect id.

    @Column(name = "business_name", nullable = false)
    private String businessName; // Business name.

    private String website; // Website URL (nullable).
    private String sector; // Sector (nullable).
    private String location; // Location (nullable).

    @Column(name = "lead_score", nullable = false)
    private int leadScore = 0; // Lead score.

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_status", nullable = false)
    private ContactStatus contactStatus = ContactStatus.NEW; // Contact status.

    private String notes; // Free-form notes (nullable).

    @Column(name = "outreach_draft")
    private String outreachDraft; // Outreach draft (nullable).

    @Column(nullable = false)
    private boolean suppressed = false; // Opt-out / suppression flag.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Creation time.

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now(); // Last update time.

    /** No-args constructor required by JPA. */
    protected Prospect() {
    }

    /** Creates a prospect in the NEW state. */
    public Prospect(String businessName, String website, String sector, String location) {
        this.businessName = businessName;
        this.website = website;
        this.sector = sector;
        this.location = location;
    }

    /** Updates mutable fields. */
    public void update(String sector, String location, int leadScore, String notes, String outreachDraft) {
        this.sector = sector;
        this.location = location;
        this.leadScore = leadScore;
        this.notes = notes;
        this.outreachDraft = outreachDraft;
        this.updatedAt = Instant.now();
    }

    /** Sets the contact status. */
    public void setStatus(ContactStatus status) {
        this.contactStatus = status;
        this.updatedAt = Instant.now();
    }

    /** Honours an opt-out: suppress and drop any outreach draft. */
    public void suppress() {
        this.suppressed = true;
        this.outreachDraft = null;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getBusinessName() { return businessName; }
    public String getWebsite() { return website; }
    public String getSector() { return sector; }
    public String getLocation() { return location; }
    public int getLeadScore() { return leadScore; }
    public ContactStatus getContactStatus() { return contactStatus; }
    public String getNotes() { return notes; }
    public String getOutreachDraft() { return outreachDraft; }
    public boolean isSuppressed() { return suppressed; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
