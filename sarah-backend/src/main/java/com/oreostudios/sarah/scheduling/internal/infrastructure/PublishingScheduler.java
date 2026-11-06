package com.oreostudios.sarah.scheduling.internal.infrastructure; // Infrastructure layer of the scheduling module.

import com.oreostudios.sarah.publishing.api.PublishingFacade; // Publishing port.
import org.slf4j.Logger; // Logging.
import org.slf4j.LoggerFactory; // Logger factory.
import org.springframework.scheduling.annotation.Scheduled; // Fixed-delay scheduling.
import org.springframework.stereotype.Component; // Spring component.

import java.time.Instant; // Current time.
import java.util.List; // Due ids.
import java.util.UUID; // Identifiers.

/**
 * Background worker that publishes due content. Each item is attempted in its own transaction
 * (via the publishing facade) so one failure never blocks the rest. The kill switch and all
 * limits are enforced inside the facade.
 */
@Component // Scheduled worker.
public class PublishingScheduler {

    private static final Logger log = LoggerFactory.getLogger(PublishingScheduler.class); // Logger.

    private final PublishingFacade publishing; // Publishing port.

    /** @param publishing publishing port */
    public PublishingScheduler(PublishingFacade publishing) {
        this.publishing = publishing;
    }

    /** Polls for due publications on a fixed delay and attempts each. */
    @Scheduled(fixedDelayString = "${sarah.scheduling.publish-interval-ms:30000}")
    public void publishDue() {
        List<UUID> due = publishing.dueIds(Instant.now()); // Find due items.
        if (due.isEmpty()) {
            return;
        }
        log.debug("Publishing scheduler: {} due item(s)", due.size());
        int published = 0;
        for (UUID id : due) { // Attempt each independently.
            try {
                if (publishing.publishOne(id)) {
                    published++;
                }
            } catch (RuntimeException e) {
                log.warn("publishOne failed for {}: {}", id, e.getMessage());
            }
        }
        if (published > 0) {
            log.info("Publishing scheduler published {} item(s)", published);
        }
    }
}
