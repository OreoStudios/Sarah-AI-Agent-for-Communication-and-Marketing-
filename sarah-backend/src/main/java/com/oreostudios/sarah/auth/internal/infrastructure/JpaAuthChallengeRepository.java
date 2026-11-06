package com.oreostudios.sarah.auth.internal.infrastructure; // Infrastructure layer of the auth module.

import com.oreostudios.sarah.auth.internal.domain.AuthChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link AuthChallenge}. */
public interface JpaAuthChallengeRepository extends JpaRepository<AuthChallenge, UUID> {

    Optional<AuthChallenge> findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(String email);

    void deleteByEmailIgnoreCase(String email);

    long deleteByExpiresAtBefore(Instant cutoff);
}
