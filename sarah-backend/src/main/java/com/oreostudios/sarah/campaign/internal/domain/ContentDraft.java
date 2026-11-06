package com.oreostudios.sarah.campaign.internal.domain; // Domain layer of the campaign module.

import com.oreostudios.sarah.campaign.api.DraftStatus; // Draft status (public enum).
import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.EnumType; // Enum persistence selector.
import jakarta.persistence.Enumerated; // Enum persistence declaration.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Creation timestamp.
import java.util.UUID; // Identifier type.

/** A platform-specific content draft produced for a campaign. */
@Entity // Managed entity.
@Table(name = "content_draft") // Backing table.
public class ContentDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Draft id.

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId; // Owning campaign.

    @Column(nullable = false)
    private String platform; // Platform code.

    @Column(nullable = false)
    private String language = "en"; // Content language code.

    private String title; // Optional title.

    @Column(nullable = false)
    private String body; // Content body.

    @Column(name = "image_brief")
    private String imageBrief; // Optional image/creative brief.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status = DraftStatus.DRAFT; // Lifecycle status.

    @Column(name = "compliance_score")
    private Integer complianceScore; // Compliance score (nullable).

    @Column(name = "compliance_notes")
    private String complianceNotes; // Compliance advisory (nullable).

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Creation time.

    /** No-args constructor required by JPA. */
    protected ContentDraft() {
    }

    /** Creates a draft in the {@link DraftStatus#DRAFT} state. */
    public ContentDraft(UUID campaignId, String platform, String language,
                        String title, String body, String imageBrief) {
        this.campaignId = campaignId;
        this.platform = platform;
        this.language = language;
        this.title = title;
        this.body = body;
        this.imageBrief = imageBrief;
        this.status = DraftStatus.DRAFT;
    }

    /**
     * Applies a compliance verdict, moving the draft to APPROVED or REJECTED.
     *
     * @param score    compliance score
     * @param notes    compliance advisory
     * @param approved whether compliance approved the draft
     */
    public void applyCompliance(int score, String notes, boolean approved) {
        this.complianceScore = score;
        this.complianceNotes = notes;
        this.status = approved ? DraftStatus.APPROVED : DraftStatus.REJECTED;
    }

    public UUID getId() { return id; }
    public UUID getCampaignId() { return campaignId; }
    public String getPlatform() { return platform; }
    public String getLanguage() { return language; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getImageBrief() { return imageBrief; }
    public DraftStatus getStatus() { return status; }
    public Integer getComplianceScore() { return complianceScore; }
    public String getComplianceNotes() { return complianceNotes; }
    public Instant getCreatedAt() { return createdAt; }
}
