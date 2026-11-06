package com.oreostudios.sarah.campaign.internal.infrastructure.web; // Web adapter of the campaign module.

import com.oreostudios.sarah.campaign.api.CampaignFacade; // Campaign port.
import com.oreostudios.sarah.campaign.api.CampaignStatus; // Workflow states.
import com.oreostudios.sarah.campaign.api.CampaignView; // Campaign DTO.
import com.oreostudios.sarah.campaign.api.CreateCampaignRequest; // Create request.
import com.oreostudios.sarah.campaign.api.DraftView; // Draft DTO.
import com.oreostudios.sarah.campaign.api.UpdateCampaignRequest; // Update request.
import jakarta.validation.Valid; // Validates request bodies.
import org.springframework.http.HttpStatus; // HTTP statuses.
import org.springframework.security.access.prepost.PreAuthorize; // Authorization.
import org.springframework.security.core.Authentication; // Current principal.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.PathVariable; // Path variables.
import org.springframework.web.bind.annotation.PostMapping; // POST mapping.
import org.springframework.web.bind.annotation.PutMapping; // PUT mapping.
import org.springframework.web.bind.annotation.RequestBody; // JSON body.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RequestParam; // Query params.
import org.springframework.web.bind.annotation.ResponseStatus; // Response status override.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

import java.util.List; // Return types.
import java.util.UUID; // Identifiers.

/** REST controller for campaigns under {@code /api/v1/campaigns}. */
@RestController // JSON controller.
@RequestMapping("/api/v1/campaigns") // Base path.
public class CampaignController {

    private final CampaignFacade campaigns; // Campaign port.

    /** @param campaigns campaign port */
    public CampaignController(CampaignFacade campaigns) {
        this.campaigns = campaigns;
    }

    /** @return all campaigns, newest first */
    @GetMapping
    public List<CampaignView> list() {
        return campaigns.list();
    }

    /** @return a single campaign */
    @GetMapping("/{id}")
    public CampaignView get(@PathVariable UUID id) {
        return campaigns.get(id);
    }

    /** @return the campaign's drafts */
    @GetMapping("/{id}/drafts")
    public List<DraftView> drafts(@PathVariable UUID id) {
        return campaigns.draftsFor(id);
    }

    /** Creates a campaign. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public CampaignView create(@Valid @RequestBody CreateCampaignRequest req, Authentication auth) {
        return campaigns.create(req, auth.getName());
    }

    /** Edits a campaign. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public CampaignView update(@PathVariable UUID id, @Valid @RequestBody UpdateCampaignRequest req, Authentication auth) {
        return campaigns.update(id, req, auth.getName());
    }

    /** Performs a workflow transition. */
    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('OWNER','OPERATOR')")
    public CampaignView transition(@PathVariable UUID id, @RequestParam CampaignStatus to, Authentication auth) {
        return campaigns.transition(id, to, auth.getName());
    }
}
