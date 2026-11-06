package com.oreostudios.sarah.connector.internal.infrastructure.discord; // Discord adapter (infrastructure).

import com.oreostudios.sarah.connector.api.Capability; // Capabilities.
import com.oreostudios.sarah.connector.api.PublishCommand; // Publish command.
import com.oreostudios.sarah.connector.api.PublishResult; // Publish result.
import com.oreostudios.sarah.connector.internal.domain.AnalyticsSnapshot; // Analytics value.
import com.oreostudios.sarah.connector.internal.domain.ConnectorContext; // Operation context.
import com.oreostudios.sarah.connector.internal.domain.PublicationStatusInfo; // Status value.
import com.oreostudios.sarah.connector.internal.domain.SocialConnector; // SPI.
import com.oreostudios.sarah.connector.internal.infrastructure.OutboundHttpGuard; // SSRF guard.
import com.oreostudios.sarah.shared.Platform; // Platform enum.
import com.oreostudios.sarah.shared.PolicyViolationException; // Policy signal.
import org.slf4j.Logger; // Logging.
import org.slf4j.LoggerFactory; // Logger factory.
import org.springframework.http.MediaType; // JSON media type.
import org.springframework.stereotype.Component; // Spring component.
import org.springframework.web.client.RestClient; // HTTP client.
import org.springframework.web.client.RestClientResponseException; // HTTP error.

import java.net.URI; // Request URI.
import java.util.Map; // JSON body/response.
import java.util.Set; // Capability set.

/**
 * Real Discord connector using an incoming webhook URL (the account secret). Webhooks are an
 * official Discord feature for authorized channels — a policy-compliant publishing path.
 */
@Component // Live connector bean.
public class DiscordWebhookConnector implements SocialConnector {

    private static final Logger log = LoggerFactory.getLogger(DiscordWebhookConnector.class); // Logger.
    private static final int DISCORD_MAX = 2000; // Discord content limit.

    private final RestClient http; // HTTP client.
    private final OutboundHttpGuard guard; // SSRF guard.

    /**
     * @param builder base RestClient builder
     * @param guard   SSRF guard
     */
    public DiscordWebhookConnector(RestClient.Builder builder, OutboundHttpGuard guard) {
        this.http = builder.build();
        this.guard = guard;
    }

    @Override
    public Platform platform() {
        return Platform.DISCORD;
    }

    @Override
    public Set<Capability> capabilities() {
        // Webhooks can publish and confirm delivery; they cannot fetch analytics.
        return Set.of(Capability.VALIDATE_CREDENTIALS, Capability.PREPARE_CONTENT,
                Capability.PUBLISH, Capability.SCHEDULE, Capability.FETCH_STATUS);
    }

    @Override
    public boolean live() {
        return true;
    }

    @Override
    public void validateCredentials(ConnectorContext ctx) {
        String webhook = ctx.secret();
        if (webhook == null || webhook.isBlank()) { // Require a webhook URL.
            throw new PolicyViolationException("Discord webhook URL is not configured");
        }
        guard.validate(webhook); // Validate scheme + host + non-internal address.
    }

    @Override
    public String prepareContent(String raw, String language) {
        String text = raw == null ? "" : raw.strip();
        if (text.length() > DISCORD_MAX) { // Clamp to Discord's limit.
            text = text.substring(0, DISCORD_MAX - 1) + "…";
        }
        return text;
    }

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishCommand request) {
        URI uri;
        try {
            URI base = guard.validate(ctx.secret()); // Validate the webhook URL.
            String sep = base.getQuery() == null ? "?" : "&"; // ?wait=true returns the created message.
            uri = URI.create(base + sep + "wait=true");
        } catch (PolicyViolationException e) {
            return PublishResult.permanentFailure("Blocked by outbound policy: " + e.getMessage());
        }

        String content = prepareContent(request.content(), request.language()); // Platform-ready content.
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = http.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content)) // JSON {"content": ...}.
                    .retrieve()
                    .body(Map.class);
            String messageId = body == null ? null : String.valueOf(body.get("id")); // Message id.
            String channelId = body == null ? null : String.valueOf(body.get("channel_id")); // Channel id.
            String url = (messageId != null && channelId != null)
                    ? "https://discord.com/channels/@me/" + channelId + "/" + messageId
                    : null;
            log.info("Published to Discord webhook, message id={}", messageId);
            return PublishResult.ok(messageId, url, "Published to Discord");
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            boolean retriable = code == 429 || code >= 500; // Rate-limit/5xx are transient.
            String msg = "Discord responded " + code + ": " + e.getResponseBodyAsString();
            return retriable ? PublishResult.transientFailure(msg) : PublishResult.permanentFailure(msg);
        } catch (Exception e) {
            return PublishResult.transientFailure("Discord request failed: " + e.getMessage()); // Network/timeout.
        }
    }

    @Override
    public PublicationStatusInfo getPublicationStatus(ConnectorContext ctx, String externalId) {
        // Webhook deliveries are confirmed at publish time; there is no polling API.
        return new PublicationStatusInfo(externalId, externalId != null ? "DELIVERED" : "UNKNOWN",
                "Discord webhook delivery is confirmed at publish time");
    }

    @Override
    public AnalyticsSnapshot fetchAnalytics(ConnectorContext ctx, String externalId) {
        return AnalyticsSnapshot.unavailable("Discord webhooks do not expose message analytics");
    }
}
