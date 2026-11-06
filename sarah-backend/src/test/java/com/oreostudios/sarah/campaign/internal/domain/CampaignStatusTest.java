package com.oreostudios.sarah.campaign.internal.domain; // Same package as the Campaign aggregate.

import com.oreostudios.sarah.campaign.api.CampaignStatus; // Workflow states.
import com.oreostudios.sarah.shared.PolicyViolationException; // Illegal-transition signal.
import org.junit.jupiter.api.Test; // JUnit 5.

import java.util.List; // Platform list.

import static org.assertj.core.api.Assertions.assertThat; // Fluent assertions.
import static org.assertj.core.api.Assertions.assertThatThrownBy; // Exception assertions.

class CampaignStatusTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        Campaign c = new Campaign("n", "o", "a", List.of("discord"), "en", 0, null, null, "owner");
        assertThat(c.getStatus()).isEqualTo(CampaignStatus.CREATED);
        c.transitionTo(CampaignStatus.PLANNED);
        c.transitionTo(CampaignStatus.REVIEWED);
        c.transitionTo(CampaignStatus.SCHEDULED);
        c.transitionTo(CampaignStatus.RUNNING);
        c.transitionTo(CampaignStatus.COMPLETED);
        assertThat(c.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
    }

    @Test
    void illegalTransitionIsRejected() {
        Campaign c = new Campaign("n", "o", "a", List.of("discord"), "en", 0, null, null, "owner");
        assertThatThrownBy(() -> c.transitionTo(CampaignStatus.RUNNING))
                .isInstanceOf(PolicyViolationException.class);
    }

    @Test
    void terminalStatesAllowNoFurtherTransition() {
        assertThat(CampaignStatus.COMPLETED.allowedNext()).isEmpty();
        assertThat(CampaignStatus.CANCELLED.allowedNext()).isEmpty();
        assertThat(CampaignStatus.COMPLETED.isTerminal()).isTrue();
    }
}
