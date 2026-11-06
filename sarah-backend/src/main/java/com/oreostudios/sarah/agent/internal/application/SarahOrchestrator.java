package com.oreostudios.sarah.agent.internal.application; // Application layer of the agent module.

import com.oreostudios.sarah.agent.api.AgentAction; // Timeline action (public).
import com.oreostudios.sarah.agent.api.ChatOutcome; // Chat result (public).
import com.oreostudios.sarah.agent.api.SarahFacade; // Implemented public port.
import com.oreostudios.sarah.agent.internal.domain.ComplianceResult; // Compliance value.
import com.oreostudios.sarah.agent.internal.domain.GeneratedContent; // Content value.
import com.oreostudios.sarah.audit.api.AuditFacade; // Audit port.
import com.oreostudios.sarah.autonomy.api.AutonomyFacade; // Autonomy port.
import com.oreostudios.sarah.brand.api.BrandFacade; // Brand port.
import com.oreostudios.sarah.brand.api.BrandView; // Brand DTO.
import com.oreostudios.sarah.campaign.api.AddDraftCommand; // Add-draft command.
import com.oreostudios.sarah.campaign.api.CampaignFacade; // Campaign port.
import com.oreostudios.sarah.campaign.api.CampaignStatus; // Workflow states.
import com.oreostudios.sarah.campaign.api.CampaignView; // Campaign DTO.
import com.oreostudios.sarah.campaign.api.CreateCampaignRequest; // Create request.
import com.oreostudios.sarah.campaign.api.DraftView; // Draft DTO.
import com.oreostudios.sarah.connector.api.ConnectorFacade; // Connector port.
import com.oreostudios.sarah.llm.api.LlmClient; // LLM port.
import com.oreostudios.sarah.llm.api.PromptSanitizer; // Prompt-injection guard.
import com.oreostudios.sarah.memory.api.ChatRole; // Conversation role.
import com.oreostudios.sarah.memory.api.ConversationFacade; // Conversation port.
import com.oreostudios.sarah.publishing.api.PublishingFacade; // Publishing port.
import com.oreostudios.sarah.publishing.api.SchedulePublicationCommand; // Schedule command.
import com.oreostudios.sarah.scheduling.api.TaskRef; // Task handle.
import com.oreostudios.sarah.scheduling.api.WorkflowTaskFacade; // Workflow-task port.
import com.oreostudios.sarah.shared.Platform; // Platform enum.
import org.slf4j.Logger; // Logging.
import org.slf4j.LoggerFactory; // Logger factory.
import org.springframework.stereotype.Service; // Spring service bean.

import java.time.Duration; // Scheduling offset.
import java.time.Instant; // Timestamps.
import java.util.ArrayList; // Mutable lists.
import java.util.List; // Lists.
import java.util.Locale; // Lower-casing.
import java.util.Optional; // Optional channel id.
import java.util.UUID; // Identifiers.

/**
 * Sarah — the central coordinator. Understands an owner instruction, breaks it into tasks,
 * delegates to specialist agents, enforces limits, and summarises what happened.
 *
 * <p>Permission/limit enforcement (kill switch, duplicate prevention, LLM-call budget, loop
 * protection) is deterministic Java. The LLM only produces conversational text and marketing
 * copy — never authorization decisions.
 */
@Service // Implements the Sarah port.
public class SarahOrchestrator implements SarahFacade {

    private static final Logger log = LoggerFactory.getLogger(SarahOrchestrator.class); // Logger.

    private static final List<String> CAMPAIGN_TRIGGERS = List.of( // Intent keywords.
            "campaign", "launch", "promote", "promotion", "market ", "marketing",
            "post about", "announce", "advertise", "publicize");

    private final ConversationFacade conversation; // Memory port.
    private final BrandFacade brand; // Brand port.
    private final CampaignFacade campaigns; // Campaign port.
    private final ContentAgent contentAgent; // Content specialist.
    private final ComplianceAgent complianceAgent; // Compliance specialist.
    private final ConnectorFacade connectors; // Connector port.
    private final PublishingFacade publishing; // Publishing port.
    private final WorkflowTaskFacade tasks; // Task-queue port.
    private final AutonomyFacade autonomy; // Autonomy port.
    private final LlmClient llm; // LLM port.
    private final PromptSanitizer sanitizer; // Prompt-injection guard.
    private final AuditFacade audit; // Audit port.

    public SarahOrchestrator(ConversationFacade conversation, BrandFacade brand, CampaignFacade campaigns,
                             ContentAgent contentAgent, ComplianceAgent complianceAgent,
                             ConnectorFacade connectors, PublishingFacade publishing,
                             WorkflowTaskFacade tasks, AutonomyFacade autonomy,
                             LlmClient llm, PromptSanitizer sanitizer, AuditFacade audit) {
        this.conversation = conversation;
        this.brand = brand;
        this.campaigns = campaigns;
        this.contentAgent = contentAgent;
        this.complianceAgent = complianceAgent;
        this.connectors = connectors;
        this.publishing = publishing;
        this.tasks = tasks;
        this.autonomy = autonomy;
        this.llm = llm;
        this.sanitizer = sanitizer;
        this.audit = audit;
    }

    @Override
    public ChatOutcome chat(UUID conversationId, String message, String actor) {
        UUID convId = conversationId != null ? conversationId : UUID.randomUUID(); // Resolve conversation.
        conversation.append(convId, ChatRole.USER, message); // Persist the user turn.

        ChatOutcome outcome = isCampaignRequest(message) // Route by intent.
                ? planAndBuildCampaign(convId, message, actor)
                : generalChat(convId, message);

        conversation.append(convId, ChatRole.ASSISTANT, outcome.reply()); // Persist the reply.
        return outcome;
    }

    // --- campaign path ----------------------------------------------------

    private ChatOutcome planAndBuildCampaign(UUID convId, String message, String actor) {
        List<AgentAction> actions = new ArrayList<>(); // Activity timeline.
        BrandView brandView = brand.active(); // Brand context.

        List<Platform> platforms = detectPlatforms(message); // Target platforms.
        String language = detectLanguage(message); // Target language.
        String audience = detectAudience(message); // Target audience.
        String objective = message.strip(); // Brief.
        String name = generateCampaignName(message); // Unique name.

        CampaignView campaign = campaigns.create(new CreateCampaignRequest( // Create the campaign.
                name, objective, audience, platforms.stream().map(Platform::code).toList(),
                language, 0L, Instant.now(), null), actor);
        actions.add(new AgentAction("Sarah", "CAMPAIGN_CREATED",
                "'" + name + "' targeting " + platforms.size() + " platform(s)"));

        int llmBudget = autonomy.current().maxLlmCallsPerRun(); // LLM-call budget (loop protection).
        int hops = 0; // Agent-hop counter.
        int scheduled = 0; // Count of scheduled publications.
        List<String> summary = new ArrayList<>(); // Per-platform summary lines.

        for (Platform platform : platforms) {
            if (hops >= llmBudget) { // Respect the budget.
                summary.add("• " + platform.displayName() + ": skipped (LLM-call budget reached)");
                break;
            }
            hops++;

            TaskRef task = tasks.create(campaign.id(), "GENERATE_CONTENT", "ContentAgent", "platform=" + platform.code());
            tasks.start(task);
            try {
                GeneratedContent gen = contentAgent.generate(brandView, objective, audience, language, platform); // Generate.
                ComplianceResult compliance = complianceAgent.evaluate(platform, language, gen.body()); // Evaluate.

                DraftView draft = campaigns.addDraft(new AddDraftCommand( // Persist the draft + verdict.
                        campaign.id(), platform.code(), language, gen.title(), gen.body(), gen.imageBrief(),
                        compliance.score(), compliance.advisory(), compliance.approved()));
                tasks.complete(task);

                actions.add(new AgentAction("ContentAgent", "DRAFT_CREATED",
                        platform.displayName() + " (" + gen.body().length() + " chars)"));
                actions.add(new AgentAction("ComplianceAgent", compliance.approved() ? "APPROVED" : "REJECTED",
                        "score " + compliance.score()));

                if (compliance.approved()) {
                    Optional<UUID> channelId = connectors.findConnectedChannelId(platform); // Connected channel?
                    if (channelId.isPresent()) {
                        schedule(campaign.id(), draft.id(), channelId.get(), platform, gen.body()); // Queue it.
                        scheduled++;
                        summary.add("• " + platform.displayName() + ": approved (score " + compliance.score()
                                + "), scheduled");
                        actions.add(new AgentAction("SocialMediaAgent", "SCHEDULED", platform.displayName()));
                    } else {
                        summary.add("• " + platform.displayName() + ": approved (score " + compliance.score()
                                + "), but no connected channel — setup required");
                    }
                } else {
                    summary.add("• " + platform.displayName() + ": needs revision (score " + compliance.score()
                            + ") — " + String.join("; ", compliance.violations()));
                }
            } catch (RuntimeException e) {
                tasks.fail(task, e.getMessage());
                summary.add("• " + platform.displayName() + ": failed (" + e.getMessage() + ")");
                log.warn("Content pipeline failed for {}: {}", platform, e.getMessage());
            }
        }

        // Workflow transitions: CREATED -> PLANNED -> REVIEWED (-> SCHEDULED if any scheduled).
        campaigns.transition(campaign.id(), CampaignStatus.PLANNED, actor);
        campaigns.transition(campaign.id(), CampaignStatus.REVIEWED, actor);
        if (scheduled > 0) {
            campaigns.transition(campaign.id(), CampaignStatus.SCHEDULED, actor);
        }

        audit.record(actor, "CHAT_CAMPAIGN_BUILT", "campaign", campaign.id().toString(),
                "platforms=" + platforms + " scheduled=" + scheduled);

        String reply = """
                Done — I created the campaign **%s** and ran it through the content and compliance agents.

                %s

                %s
                Scheduled publications: %d. You can review drafts and the content calendar in the dashboard.
                """.formatted(name, String.join("\n", summary),
                scheduled == 0
                        ? "No publications were scheduled automatically (connect a channel to enable publishing)."
                        : "Approved content is queued; the kill switch and daily limits still apply at publish time.",
                scheduled);

        return new ChatOutcome(convId, reply, campaign.id(), List.copyOf(actions), llm.providerName());
    }

    /** Idempotently schedules an approved draft to a connected channel. */
    private void schedule(UUID campaignId, UUID draftId, UUID channelId, Platform platform, String content) {
        String idempotencyKey = campaignId + ":" + platform.code() + ":" + draftId; // Deterministic key.
        publishing.schedule(new SchedulePublicationCommand(campaignId, draftId, channelId, platform, content,
                Instant.now().plus(Duration.ofMinutes(5)), 3, idempotencyKey));
    }

    // --- general chat path ------------------------------------------------

    private ChatOutcome generalChat(UUID convId, String message) {
        BrandView brandView = brand.active(); // Brand context.
        String system = """
                %s

                You are Sarah, the autonomous marketing assistant for %s.
                %s
                Services: %s
                Be concise, professional and honest. If asked to do something requiring publishing,
                explain that the owner can ask you to create a campaign. Never fabricate results.
                """.formatted(sanitizer.guardClause(), brandView.companyName(), brandView.description(),
                String.join(", ", brandView.serviceList()));

        String history = conversation.recentWindow(convId, 10).stream()
                .map(t -> t.role() + ": " + t.content())
                .reduce((a, b) -> a + "\n" + b).orElse(""); // Short-term context.
        String user = (history.isEmpty() ? "" : "Recent conversation:\n" + history + "\n\n")
                + "Owner: " + sanitizer.wrapUntrusted("owner-message", message);

        String reply = llm.generate(system, user).strip(); // Conversational reply.
        return new ChatOutcome(convId, reply, null, List.of(), llm.providerName());
    }

    // --- deterministic parsing helpers -----------------------------------

    private boolean isCampaignRequest(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return CAMPAIGN_TRIGGERS.stream().anyMatch(lower::contains);
    }

    private List<Platform> detectPlatforms(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        List<Platform> found = new ArrayList<>();
        for (Platform p : Platform.values()) {
            if (lower.contains(p.code()) || lower.contains(p.name().toLowerCase(Locale.ROOT))
                    || lower.contains(p.displayName().toLowerCase(Locale.ROOT))) {
                found.add(p);
            }
        }
        if (found.isEmpty()) { // Sensible default: LinkedIn + Discord (the connector that truly publishes).
            found.add(Platform.LINKEDIN);
            found.add(Platform.DISCORD);
        }
        return found;
    }

    private String detectLanguage(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return (lower.contains("french") || lower.contains("français") || lower.contains("francais")) ? "fr" : "en";
    }

    private String detectAudience(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(" for ");
        if (idx >= 0) {
            String tail = message.substring(idx + 5).strip();
            String audience = tail.split("[.!?\\n]", 2)[0].strip();
            if (!audience.isEmpty() && audience.length() <= 200) {
                return audience;
            }
        }
        return "SMBs, entrepreneurs, and software teams";
    }

    private String generateCampaignName(String message) {
        String words = message.strip().replaceAll("[^a-zA-Z0-9 ]", " ").replaceAll("\\s+", " ").strip();
        String head = words.isEmpty() ? "Campaign" : words.substring(0, Math.min(40, words.length())).strip();
        String name = head + " - " + UUID.randomUUID().toString().substring(0, 8); // Ensure uniqueness.
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
