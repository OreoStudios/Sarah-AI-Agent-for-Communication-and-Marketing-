package com.oreostudios.sarah.auth.internal.domain; // Domain layer of the auth module.

import java.util.Optional;

/** Secondary port for persisting accounts. */
public interface AccountStore {
    Optional<UserAccount> findByEmail(String email);
    UserAccount save(UserAccount account);
}
