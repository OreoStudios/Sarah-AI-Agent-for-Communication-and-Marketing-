package com.oreostudios.sarah.campaign.api; // Public API package of the campaign module.

import java.util.EnumSet; // Transition sets.
import java.util.Set; // Return type.

/**
 * Campaign workflow states.
 *
 * <pre>CREATED → PLANNED → REVIEWED → SCHEDULED → RUNNING → COMPLETED</pre>
 *
 * Plus the branch states AWAITING_APPROVAL, FAILED, CANCELLED, PAUSED. Transitions are enforced
 * deterministically in the campaign aggregate.
 */
public enum CampaignStatus {
    CREATED, PLANNED, REVIEWED, SCHEDULED, RUNNING, COMPLETED, // Main path.
    AWAITING_APPROVAL, FAILED, CANCELLED, PAUSED; // Branch states.

    private static final Set<CampaignStatus> TERMINAL = EnumSet.of(COMPLETED, CANCELLED); // Terminal states.

    /** @return whether this is a terminal state */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** @return the set of states reachable from this one */
    public Set<CampaignStatus> allowedNext() {
        return switch (this) {
            case CREATED -> EnumSet.of(PLANNED, AWAITING_APPROVAL, CANCELLED, FAILED);
            case PLANNED -> EnumSet.of(REVIEWED, AWAITING_APPROVAL, CANCELLED, FAILED);
            case REVIEWED -> EnumSet.of(SCHEDULED, AWAITING_APPROVAL, CANCELLED, FAILED);
            case SCHEDULED -> EnumSet.of(RUNNING, PAUSED, CANCELLED, FAILED);
            case RUNNING -> EnumSet.of(COMPLETED, PAUSED, FAILED, CANCELLED);
            case AWAITING_APPROVAL -> EnumSet.of(PLANNED, REVIEWED, SCHEDULED, CANCELLED, FAILED);
            case PAUSED -> EnumSet.of(SCHEDULED, RUNNING, CANCELLED);
            case FAILED -> EnumSet.of(PLANNED, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(CampaignStatus.class);
        };
    }

    /** @return whether a transition to {@code next} is allowed */
    public boolean canTransitionTo(CampaignStatus next) {
        return allowedNext().contains(next);
    }
}
