package com.oreostudios.sarah.prospecting.internal.application; // Application layer of the prospecting module.

import com.oreostudios.sarah.audit.api.AuditFacade; // Audit port.
import com.oreostudios.sarah.prospecting.internal.domain.Prospect; // Entity.
import com.oreostudios.sarah.prospecting.internal.domain.Prospect.ContactStatus; // Status enum.
import com.oreostudios.sarah.prospecting.internal.infrastructure.JpaProspectRepository; // Repository.
import com.oreostudios.sarah.shared.NotFoundException; // 404 signal.
import com.oreostudios.sarah.shared.PolicyViolationException; // Suppression guard.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

import java.time.Instant; // Timestamp in DTO.
import java.util.List; // Return type.
import java.util.UUID; // Identifiers.

/** CRM operations over prospects, including GDPR suppression. */
@Service // Spring-managed service.
@Transactional // Default transactional.
public class ProspectService {

    /** CRM projection of a prospect. */
    public record ProspectView(UUID id, String businessName, String website, String sector, String location,
                               int leadScore, ContactStatus contactStatus, String notes, String outreachDraft,
                               boolean suppressed, Instant updatedAt) {
    }

    /** Request to add a prospect. */
    public record CreateProspectRequest(String businessName, String website, String sector, String location) {
    }

    private final JpaProspectRepository repo; // Persistence gateway.
    private final AuditFacade audit; // Audit port.

    ProspectService(JpaProspectRepository repo, AuditFacade audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /** Creates a prospect with a heuristic lead score. */
    public ProspectView create(CreateProspectRequest req, String actor) {
        Prospect p = new Prospect(req.businessName(), req.website(), req.sector(), req.location());
        p.update(req.sector(), req.location(), score(req), null, null); // Apply the heuristic score.
        p = repo.save(p);
        audit.record(actor, "PROSPECT_CREATED", "prospect", p.getId().toString(), req.businessName());
        return toView(p);
    }

    /** @return all prospects by descending lead score */
    @Transactional(readOnly = true)
    public List<ProspectView> list() {
        return repo.findAllByOrderByLeadScoreDesc().stream().map(ProspectService::toView).toList();
    }

    /** Sets a prospect's contact status (rejected while suppressed, except LOST). */
    public ProspectView setStatus(UUID id, ContactStatus status, String actor) {
        Prospect p = require(id);
        if (p.isSuppressed() && status != ContactStatus.LOST) {
            throw new PolicyViolationException("Prospect is suppressed (opt-out); no outreach status changes allowed");
        }
        p.setStatus(status);
        audit.record(actor, "PROSPECT_STATUS", "prospect", id.toString(), status.name());
        return toView(p);
    }

    /** Honours an opt-out / suppression request (GDPR). */
    public ProspectView suppress(UUID id, String actor) {
        Prospect p = require(id);
        p.suppress();
        audit.record(actor, "PROSPECT_SUPPRESSED", "prospect", id.toString(), null);
        return toView(p);
    }

    private int score(CreateProspectRequest req) {
        int s = 40; // Baseline.
        if (req.website() != null && !req.website().isBlank()) s += 20; // Has a site.
        if (req.sector() != null && !req.sector().isBlank()) s += 20; // Known sector.
        if (req.location() != null && !req.location().isBlank()) s += 10; // Known location.
        return Math.min(100, s);
    }

    private Prospect require(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Prospect not found: " + id));
    }

    private static ProspectView toView(Prospect p) {
        return new ProspectView(p.getId(), p.getBusinessName(), p.getWebsite(), p.getSector(), p.getLocation(),
                p.getLeadScore(), p.getContactStatus(), p.getNotes(), p.getOutreachDraft(),
                p.isSuppressed(), p.getUpdatedAt());
    }
}
