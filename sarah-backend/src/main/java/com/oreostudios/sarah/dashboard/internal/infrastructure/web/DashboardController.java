package com.oreostudios.sarah.dashboard.internal.infrastructure.web; // Web adapter of the dashboard module.

import com.oreostudios.sarah.audit.api.AuditEntryView; // Audit DTO.
import com.oreostudios.sarah.audit.api.AuditFacade; // Audit port.
import com.oreostudios.sarah.autonomy.api.AutonomyFacade; // Autonomy port.
import com.oreostudios.sarah.autonomy.api.AutonomyView; // Autonomy DTO.
import com.oreostudios.sarah.campaign.api.CampaignFacade; // Campaign port.
import com.oreostudios.sarah.campaign.api.CampaignView; // Campaign DTO.
import com.oreostudios.sarah.connector.api.ChannelView; // Channel DTO.
import com.oreostudios.sarah.connector.api.ConnectorFacade; // Connector port.
import com.oreostudios.sarah.llm.api.LlmClient; // LLM port.
import com.oreostudios.sarah.publishing.api.PublicationView; // Publication DTO.
import com.oreostudios.sarah.publishing.api.PublishingFacade; // Publishing port.
import org.springframework.web.bind.annotation.GetMapping; // GET mapping.
import org.springframework.web.bind.annotation.RequestMapping; // Base path.
import org.springframework.web.bind.annotation.RestController; // JSON REST controller.

import java.time.Duration; // Calendar window.
import java.time.Instant; // Current time.
import java.util.List; // Lists.
import java.util.Map; // Count map.
import java.util.TreeMap; // Ordered counts.
import java.util.stream.Collectors; // Grouping.

/** Aggregated dashboard status under {@code /api/v1/dashboard}. */
@RestController // JSON controller.
@RequestMapping("/api/v1/dashboard") // Base path.
public class DashboardController {

    /**
     * Aggregated home-screen summary.
     *
     * @param llmProvider             active LLM provider
     * @param llmLive                 whether a real model is wired
     * @param autonomyEnabled         kill-switch state
     * @param autonomy                autonomy snapshot
     * @param campaignCountsByStatus  campaign counts keyed by status
     * @param activeCampaigns         non-terminal campaigns
     * @param upcomingPublications    publications scheduled in the next 30 days
     * @param channels                connector channels
     * @param recentActivity          recent audit entries
     */
    public record Summary(
            String llmProvider,
            boolean llmLive,
            boolean autonomyEnabled,
            AutonomyView autonomy,
            Map<String, Long> campaignCountsByStatus,
            List<CampaignView> activeCampaigns,
            List<PublicationView> upcomingPublications,
            List<ChannelView> channels,
            List<AuditEntryView> recentActivity
    ) {
    }

    private final CampaignFacade campaigns; // Campaign port.
    private final PublishingFacade publishing; // Publishing port.
    private final ConnectorFacade connectors; // Connector port.
    private final AutonomyFacade autonomy; // Autonomy port.
    private final AuditFacade audit; // Audit port.
    private final LlmClient llm; // LLM port.

    public DashboardController(CampaignFacade campaigns, PublishingFacade publishing, ConnectorFacade connectors,
                               AutonomyFacade autonomy, AuditFacade audit, LlmClient llm) {
        this.campaigns = campaigns;
        this.publishing = publishing;
        this.connectors = connectors;
        this.autonomy = autonomy;
        this.audit = audit;
        this.llm = llm;
    }

    /** @return the aggregated home-screen summary */
    @GetMapping("/summary")
    public Summary summary() {
        List<CampaignView> all = campaigns.list(); // All campaigns.
        Map<String, Long> counts = all.stream() // Counts by status.
                .collect(Collectors.groupingBy(c -> c.status().name(), TreeMap::new, Collectors.counting()));
        List<CampaignView> active = all.stream() // Non-terminal, limited.
                .filter(c -> !c.status().isTerminal())
                .limit(10)
                .toList();

        Instant now = Instant.now();
        List<PublicationView> upcoming = publishing.calendar(now, now.plus(Duration.ofDays(30))); // Next 30 days.

        return new Summary(
                llm.providerName(), llm.isLive(),
                autonomy.isEnabled(), autonomy.current(),
                counts, active, upcoming,
                connectors.listChannels(),
                audit.recent(0, 15));
    }
}
