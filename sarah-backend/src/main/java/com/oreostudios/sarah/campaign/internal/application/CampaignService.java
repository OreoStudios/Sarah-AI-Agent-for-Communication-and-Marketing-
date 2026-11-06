package com.oreostudios.sarah.campaign.internal.application; // Application layer of the campaign module.

import com.oreostudios.sarah.audit.api.AuditFacade; // Audit port.
import com.oreostudios.sarah.campaign.api.AddDraftCommand; // Add-draft command.
import com.oreostudios.sarah.campaign.api.CampaignEvents; // Domain events.
import com.oreostudios.sarah.campaign.api.CampaignFacade; // Implemented public port.
import com.oreostudios.sarah.campaign.api.CampaignStatus; // Workflow states.
import com.oreostudios.sarah.campaign.api.CampaignView; // Campaign DTO.
import com.oreostudios.sarah.campaign.api.CreateCampaignRequest; // Create request.
import com.oreostudios.sarah.campaign.api.DraftView; // Draft DTO.
import com.oreostudios.sarah.campaign.api.UpdateCampaignRequest; // Update request.
import com.oreostudios.sarah.campaign.internal.domain.Campaign; // Campaign entity.
import com.oreostudios.sarah.campaign.internal.domain.ContentDraft; // Draft entity.
import com.oreostudios.sarah.campaign.internal.infrastructure.JpaCampaignRepository; // Campaign repo.
import com.oreostudios.sarah.campaign.internal.infrastructure.JpaContentDraftRepository; // Draft repo.
import com.oreostudios.sarah.shared.Platform; // Platform normalisation.
import com.oreostudios.sarah.shared.NotFoundException; // 404 signal.
import com.oreostudios.sarah.shared.PolicyViolationException; // Duplicate-name signal.
import org.springframework.context.ApplicationEventPublisher; // Publishes domain events.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

import java.util.List; // Return types.
import java.util.UUID; // Identifiers.

/** Campaign CRUD, workflow transitions, and draft persistence. */
@Service // Implements the campaign port.
public class CampaignService implements CampaignFacade {

    private final JpaCampaignRepository campaigns; // Campaign repository.
    private final JpaContentDraftRepository drafts; // Draft repository.
    private final AuditFacade audit; // Audit port.
    private final ApplicationEventPublisher events; // Event publisher.

    CampaignService(JpaCampaignRepository campaigns, JpaContentDraftRepository drafts,
                    AuditFacade audit, ApplicationEventPublisher events) {
        this.campaigns = campaigns;
        this.drafts = drafts;
        this.audit = audit;
        this.events = events;
    }

    @Override
    @Transactional
    public CampaignView create(CreateCampaignRequest req, String actor) {
        if (campaigns.existsByNameIgnoreCase(req.name())) { // Prevent duplicate campaigns.
            throw new PolicyViolationException("A campaign named '" + req.name() + "' already exists");
        }
        List<String> platforms = normalizePlatforms(req.platforms()); // Normalise platform codes.
        String language = blankToDefault(req.language()); // Default language.
        Campaign c = new Campaign(req.name().strip(), req.objective(), req.targetAudience(),
                platforms, language, req.budgetCents(), req.startAt(), req.endAt(), actor);
        c = campaigns.save(c); // Persist.
        audit.record(actor, "CAMPAIGN_CREATED", "campaign", c.getId().toString(),
                "name=" + c.getName() + " platforms=" + platforms);
        events.publishEvent(new CampaignEvents.CampaignCreated(c.getId(), c.getName(), actor)); // Notify.
        return CampaignMapper.toView(c);
    }

    @Override
    @Transactional
    public CampaignView update(UUID id, UpdateCampaignRequest req, String actor) {
        Campaign c = require(id); // Load.
        c.edit(req.objective(), req.targetAudience(), normalizePlatforms(req.platforms()),
                blankToDefault(req.language()), req.budgetCents(), req.startAt(), req.endAt()); // Apply edits.
        audit.record(actor, "CAMPAIGN_UPDATED", "campaign", id.toString(), null);
        return CampaignMapper.toView(c);
    }

    @Override
    @Transactional
    public CampaignView transition(UUID id, CampaignStatus next, String actor) {
        Campaign c = require(id); // Load.
        CampaignStatus from = c.getStatus(); // Remember previous state.
        c.transitionTo(next); // Validated transition.
        audit.record(actor, "CAMPAIGN_TRANSITION", "campaign", id.toString(), from + "->" + next);
        events.publishEvent(new CampaignEvents.CampaignStatusChanged(id, from, next)); // Notify.
        return CampaignMapper.toView(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignView> list() {
        return campaigns.findAllByOrderByCreatedAtDesc().stream().map(CampaignMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignView get(UUID id) {
        return CampaignMapper.toView(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DraftView> draftsFor(UUID campaignId) {
        return drafts.findByCampaignIdOrderByCreatedAtAsc(campaignId).stream().map(CampaignMapper::toView).toList();
    }

    @Override
    @Transactional
    public DraftView addDraft(AddDraftCommand cmd) {
        require(cmd.campaignId()); // Ensure the campaign exists.
        ContentDraft draft = new ContentDraft(cmd.campaignId(), Platform.fromCode(cmd.platform()).code(),
                cmd.language(), cmd.title(), cmd.body(), cmd.imageBrief()); // Build the draft.
        draft.applyCompliance(cmd.complianceScore(), cmd.complianceNotes(), cmd.approved()); // Apply verdict.
        return CampaignMapper.toView(drafts.save(draft)); // Persist + project.
    }

    private Campaign require(UUID id) {
        return campaigns.findById(id).orElseThrow(() -> new NotFoundException("Campaign not found: " + id));
    }

    private List<String> normalizePlatforms(List<String> raw) {
        return raw.stream().map(p -> Platform.fromCode(p).code()).distinct().toList(); // Validate + dedupe.
    }

    private String blankToDefault(String language) {
        return (language == null || language.isBlank()) ? "en" : language; // Default language.
    }
}
