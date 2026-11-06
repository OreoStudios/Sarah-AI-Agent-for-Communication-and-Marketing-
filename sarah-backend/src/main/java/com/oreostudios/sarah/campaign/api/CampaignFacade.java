package com.oreostudios.sarah.campaign.api; // Public API package of the campaign module.

import java.util.List; // Return types.
import java.util.UUID; // Identifiers.

/** Public port for campaign management, workflow transitions and draft access. */
public interface CampaignFacade {

    /**
     * Creates a campaign. Rejects a duplicate name.
     *
     * @param request campaign data
     * @param actor   who created it
     * @return the created campaign
     */
    CampaignView create(CreateCampaignRequest request, String actor);

    /**
     * Edits a campaign's mutable fields.
     *
     * @param id      campaign id
     * @param request new values
     * @param actor   who performed the edit
     * @return the updated campaign
     */
    CampaignView update(UUID id, UpdateCampaignRequest request, String actor);

    /**
     * Performs a validated workflow transition.
     *
     * @param id    campaign id
     * @param next  target state
     * @param actor who performed the transition
     * @return the updated campaign
     */
    CampaignView transition(UUID id, CampaignStatus next, String actor);

    /** @return all campaigns, newest first */
    List<CampaignView> list();

    /**
     * @param id campaign id
     * @return the campaign
     */
    CampaignView get(UUID id);

    /**
     * @param campaignId campaign id
     * @return the campaign's drafts in creation order
     */
    List<DraftView> draftsFor(UUID campaignId);

    /**
     * Persists a content draft with its compliance verdict.
     *
     * @param command the draft data + verdict
     * @return the stored draft
     */
    DraftView addDraft(AddDraftCommand command);
}
