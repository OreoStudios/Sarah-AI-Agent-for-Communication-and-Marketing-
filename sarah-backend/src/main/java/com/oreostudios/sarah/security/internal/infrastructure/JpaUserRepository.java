package com.oreostudios.sarah.security.internal.infrastructure; // Infrastructure layer of the security module.

import com.oreostudios.sarah.security.internal.domain.AppUser; // Managed entity.
import org.springframework.data.jpa.repository.JpaRepository; // CRUD base.

import java.util.Optional; // Optional lookup.
import java.util.UUID; // Primary-key type.

/** Spring Data repository for {@link AppUser}. */
public interface JpaUserRepository extends JpaRepository<AppUser, UUID> {
    /** @return the user with the given email, case-insensitive */
    Optional<AppUser> findByEmailIgnoreCase(String email);
}
