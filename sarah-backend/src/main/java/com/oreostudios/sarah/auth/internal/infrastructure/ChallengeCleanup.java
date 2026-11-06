package com.oreostudios.sarah.auth.internal.infrastructure; // Infrastructure layer of the auth module.

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Periodically purges expired verification challenges so stale rows don't accumulate. */
@Component
public class ChallengeCleanup {

    private final JpaAuthChallengeRepository repo;

    public ChallengeCleanup(JpaAuthChallengeRepository repo) {
        this.repo = repo;
    }

    @Scheduled(fixedDelayString = "${sarah.auth.cleanup-interval-ms:900000}")
    @Transactional
    public void purgeExpired() {
        // Keep a short grace window past expiry before deleting.
        repo.deleteByExpiresAtBefore(Instant.now().minus(Duration.ofHours(1)));
    }
}
