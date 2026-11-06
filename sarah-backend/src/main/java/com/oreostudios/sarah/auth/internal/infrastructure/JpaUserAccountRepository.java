package com.oreostudios.sarah.auth.internal.infrastructure; // Infrastructure layer of the auth module.

import com.oreostudios.sarah.auth.internal.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for {@link UserAccount}. */
public interface JpaUserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
