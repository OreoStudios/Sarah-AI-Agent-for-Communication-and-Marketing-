package com.oreostudios.sarah.publishing.api; // Public API package of the publishing module.

import java.time.Instant; // Time window / scheduling.
import java.util.List; // Return types.
import java.util.UUID; // Identifiers.

/** Public port for scheduling, executing and inspecting publications. */
public interface PublishingFacade {

    /**
     * Schedules a publication. Idempotent: an existing key returns the existing publication.
     *
     * @param command the publication to schedule
     * @return the scheduled (or pre-existing) publication
     */
    PublicationView schedule(SchedulePublicationCommand command);

    /**
     * @param now the current time
     * @return ids of publications due at or before {@code now}
     */
    List<UUID> dueIds(Instant now);

    /**
     * Attempts to publish a single due item in its own transaction, enforcing the kill switch and
     * limits before calling the connector.
     *
     * @param id publication id
     * @return true if it actually published externally
     */
    boolean publishOne(UUID id);

    /** Cancels a non-published publication. */
    PublicationView cancel(UUID id, String actor);

    /** Re-queues a FAILED publication for another attempt (no duplicate post). */
    PublicationView retry(UUID id, String actor);

    /** @return the campaign's publications */
    List<PublicationView> forCampaign(UUID campaignId);

    /** @return publications scheduled within the window */
    List<PublicationView> calendar(Instant from, Instant to);

    /** @return publications in the given status, newest first */
    List<PublicationView> history(PublicationStatus status);

    /** @return a single publication */
    PublicationView get(UUID id);
}
