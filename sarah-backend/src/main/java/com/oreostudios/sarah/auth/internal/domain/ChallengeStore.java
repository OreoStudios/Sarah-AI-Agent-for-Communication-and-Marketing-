package com.oreostudios.sarah.auth.internal.domain; // Domain layer of the auth module.

import java.util.Optional;

/** Secondary port for persisting challenges — lets the application layer stay DB-agnostic (and testable). */
public interface ChallengeStore {
    Optional<AuthChallenge> findLatestByEmail(String email);
    AuthChallenge save(AuthChallenge challenge);
    void deleteByEmail(String email);
}
