package com.oreostudios.sarah.publishing.internal.infrastructure; // Infrastructure layer of the publishing module.

import com.oreostudios.sarah.publishing.api.PublicationStatus; // Status filter.
import com.oreostudios.sarah.publishing.internal.domain.Publication; // Managed entity.
import com.oreostudios.sarah.shared.Platform; // Platform filter.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.time.Instant; // Time filters.
import java.util.List; // Return types.
import java.util.Optional; // Optional lookup.
import java.util.UUID; // Key/campaign id type.

/** Spring Data repository for {@link Publication}. */
public interface JpaPublicationRepository extends JpaRepository<Publication, UUID> {

    /** @return the publication with this idempotency key, if any */
    Optional<Publication> findByIdempotencyKey(String idempotencyKey);

    /** @return SCHEDULED publications due at or before the cutoff, earliest first */
    List<Publication> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            PublicationStatus status, Instant cutoff);

    /** @return a campaign's publications in schedule order */
    List<Publication> findByCampaignIdOrderByScheduledAtAsc(UUID campaignId);

    /** @return publications in a status, newest first */
    List<Publication> findByStatusOrderByScheduledAtDesc(PublicationStatus status);

    /** @return count of publications in a status published after the given instant */
    long countByStatusAndPublishedAtAfter(PublicationStatus status, Instant after);

    /** @return the most recently published item for a platform in a status */
    Optional<Publication> findFirstByPlatformAndStatusOrderByPublishedAtDesc(
            Platform platform, PublicationStatus status);

    /** @return publications scheduled within a window, earliest first */
    List<Publication> findByScheduledAtBetweenOrderByScheduledAtAsc(Instant from, Instant to);
}
