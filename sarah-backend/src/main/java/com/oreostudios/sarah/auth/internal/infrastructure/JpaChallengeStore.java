package com.oreostudios.sarah.auth.internal.infrastructure; // Infrastructure layer of the auth module.

import com.oreostudios.sarah.auth.internal.domain.AuthChallenge;
import com.oreostudios.sarah.auth.internal.domain.ChallengeStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** JPA-backed {@link ChallengeStore}. */
@Repository
public class JpaChallengeStore implements ChallengeStore {

    private final JpaAuthChallengeRepository repo;

    public JpaChallengeStore(JpaAuthChallengeRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<AuthChallenge> findLatestByEmail(String email) {
        return repo.findFirstByEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Override
    public AuthChallenge save(AuthChallenge challenge) {
        return repo.save(challenge);
    }

    @Override
    public void deleteByEmail(String email) {
        repo.deleteByEmailIgnoreCase(email);
    }
}
