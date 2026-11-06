package com.oreostudios.sarah.publishing.internal.application; // Application layer of the publishing module.

import com.oreostudios.sarah.audit.api.AuditFacade; // Audit port.
import com.oreostudios.sarah.autonomy.api.AutonomyFacade; // Autonomy port.
import com.oreostudios.sarah.autonomy.api.AutonomyView; // Autonomy snapshot.
import com.oreostudios.sarah.connector.api.ConnectorFacade; // Connector port.
import com.oreostudios.sarah.connector.api.PublishCommand; // Publish command.
import com.oreostudios.sarah.connector.api.PublishResult; // Publish result.
import com.oreostudios.sarah.publishing.api.PublicationStatus; // Status enum.
import com.oreostudios.sarah.publishing.api.PublicationView; // Public DTO.
import com.oreostudios.sarah.publishing.api.PublishingFacade; // Implemented public port.
import com.oreostudios.sarah.publishing.api.SchedulePublicationCommand; // Schedule command.
import com.oreostudios.sarah.publishing.internal.domain.Publication; // Entity.
import com.oreostudios.sarah.publishing.internal.infrastructure.JpaPublicationRepository; // Repository.
import com.oreostudios.sarah.shared.Platform; // Platform enum.
import com.oreostudios.sarah.shared.NotFoundException; // 404 signal.
import com.oreostudios.sarah.shared.PolicyViolationException; // Policy signal.
import org.slf4j.Logger; // Logging.
import org.slf4j.LoggerFactory; // Logger factory.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Propagation; // Propagation modes.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

import java.time.Duration; // Backoff / cooldown.
import java.time.Instant; // Timestamps.
import java.time.temporal.ChronoUnit; // Start-of-day truncation.
import java.util.List; // Return types.
import java.util.UUID; // Identifiers.

/**
 * Schedules and executes publications with idempotency, deterministic kill-switch + daily-limit +
 * per-platform-cooldown enforcement, and retry with exponential backoff.
 */
@Service // Implements the publishing port.
public class PublishingService implements PublishingFacade {

    private static final Logger log = LoggerFactory.getLogger(PublishingService.class); // Logger.

    private final JpaPublicationRepository repo; // Persistence gateway.
    private final ConnectorFacade connectors; // Connector port.
    private final AutonomyFacade autonomy; // Autonomy port.
    private final AuditFacade audit; // Audit port.

    PublishingService(JpaPublicationRepository repo, ConnectorFacade connectors,
                      AutonomyFacade autonomy, AuditFacade audit) {
        this.repo = repo;
        this.connectors = connectors;
        this.autonomy = autonomy;
        this.audit = audit;
    }

    @Override
    @Transactional
    public PublicationView schedule(SchedulePublicationCommand cmd) {
        Publication p = repo.findByIdempotencyKey(cmd.idempotencyKey()).orElseGet(() -> { // Idempotent.
            Publication fresh = new Publication(cmd.campaignId(), cmd.draftId(), cmd.connectorAccountId(),
                    cmd.platform(), cmd.content(), cmd.scheduledAt(), cmd.maxAttempts(), cmd.idempotencyKey());
            fresh = repo.save(fresh);
            audit.record("system", "PUBLICATION_SCHEDULED", "publication", fresh.getId().toString(),
                    "platform=" + cmd.platform() + " at=" + cmd.scheduledAt());
            return fresh;
        });
        return PublicationMapper.toView(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> dueIds(Instant now) {
        return repo.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(PublicationStatus.SCHEDULED, now)
                .stream().map(Publication::getId).toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Isolated per-item transaction.
    public boolean publishOne(UUID id) {
        Publication p = repo.findById(id).orElse(null); // Load.
        if (p == null || p.getStatus() != PublicationStatus.SCHEDULED) { // Only act on SCHEDULED.
            return false;
        }
        Instant now = Instant.now();
        if (p.getScheduledAt().isAfter(now)) { // Not due yet.
            return false;
        }

        AutonomyView settings = autonomy.current(); // Live limits.
        if (!settings.enabled()) { // Kill switch -> leave SCHEDULED, consume no attempt.
            log.debug("Kill switch engaged; leaving publication {} scheduled", id);
            return false;
        }
        if (dailyLimitReached(settings, now)) { // Daily cap -> defer.
            log.info("Daily post limit ({}) reached; deferring publication {}", settings.dailyPostLimit(), id);
            return false;
        }
        Duration cooldownLeft = cooldownRemaining(p.getPlatform(), settings, now); // Per-platform cooldown.
        if (!cooldownLeft.isZero()) {
            p.markRetry("Platform cooldown active", now.plus(cooldownLeft));
            return false;
        }

        p.markPublishing(); // Claim for an attempt.
        PublishResult result;
        try {
            result = connectors.publish(p.getConnectorAccountId(),
                    new PublishCommand(p.getContent(), null, p.getIdempotencyKey(), null));
        } catch (RuntimeException e) {
            result = PublishResult.transientFailure("Connector error: " + e.getMessage());
        }

        if (result.success()) { // Published.
            p.markPublished(result.externalId(), result.externalUrl());
            audit.record("system", "PUBLICATION_PUBLISHED", "publication", id.toString(),
                    "platform=" + p.getPlatform() + " externalId=" + result.externalId());
            return true;
        }

        if (result.retriable() && p.attemptsRemaining()) { // Transient -> backoff retry.
            Instant next = now.plus(backoff(p.getAttempts()));
            p.markRetry(result.message(), next);
            audit.record("system", "PUBLICATION_RETRY", "publication", id.toString(),
                    "attempt=" + p.getAttempts() + " next=" + next);
        } else { // Permanent / exhausted.
            p.markFailed(result.message());
            audit.record("system", "PUBLICATION_FAILED", "publication", id.toString(), result.message());
        }
        return false;
    }

    @Override
    @Transactional
    public PublicationView cancel(UUID id, String actor) {
        Publication p = require(id);
        if (p.getStatus() == PublicationStatus.PUBLISHED) { // Cannot un-publish.
            throw new PolicyViolationException("Already published; cannot cancel");
        }
        p.markCancelled();
        audit.record(actor, "PUBLICATION_CANCELLED", "publication", id.toString(), null);
        return PublicationMapper.toView(p);
    }

    @Override
    @Transactional
    public PublicationView retry(UUID id, String actor) {
        Publication p = require(id);
        if (p.getStatus() != PublicationStatus.FAILED) { // Only FAILED can be retried.
            throw new PolicyViolationException("Only FAILED publications can be retried (was " + p.getStatus() + ")");
        }
        p.markRetry("Manual retry requested", Instant.now());
        audit.record(actor, "PUBLICATION_RETRY_MANUAL", "publication", id.toString(), null);
        return PublicationMapper.toView(p);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicationView> forCampaign(UUID campaignId) {
        return repo.findByCampaignIdOrderByScheduledAtAsc(campaignId).stream().map(PublicationMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicationView> calendar(Instant from, Instant to) {
        return repo.findByScheduledAtBetweenOrderByScheduledAtAsc(from, to).stream().map(PublicationMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicationView> history(PublicationStatus status) {
        return repo.findByStatusOrderByScheduledAtDesc(status).stream().map(PublicationMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicationView get(UUID id) {
        return PublicationMapper.toView(require(id));
    }

    // --- deterministic policy helpers -------------------------------------

    private boolean dailyLimitReached(AutonomyView settings, Instant now) {
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS); // UTC day boundary.
        long published = repo.countByStatusAndPublishedAtAfter(PublicationStatus.PUBLISHED, startOfDay);
        return published >= settings.dailyPostLimit();
    }

    private Duration cooldownRemaining(Platform platform, AutonomyView settings, Instant now) {
        if (settings.perPlatformCooldownMinutes() <= 0) { // Cooldown disabled.
            return Duration.ZERO;
        }
        return repo.findFirstByPlatformAndStatusOrderByPublishedAtDesc(platform, PublicationStatus.PUBLISHED)
                .map(last -> {
                    Instant allowedAt = last.getPublishedAt().plus(Duration.ofMinutes(settings.perPlatformCooldownMinutes()));
                    return allowedAt.isAfter(now) ? Duration.between(now, allowedAt) : Duration.ZERO;
                })
                .orElse(Duration.ZERO);
    }

    private Duration backoff(int attempts) {
        long minutes = (long) Math.pow(2, Math.max(0, attempts - 1)); // 1, 2, 4, ...
        return Duration.ofMinutes(Math.min(minutes, 60)); // Capped at 1h.
    }

    private Publication require(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Publication not found: " + id));
    }
}
