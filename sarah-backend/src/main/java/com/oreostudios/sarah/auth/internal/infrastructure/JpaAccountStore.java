package com.oreostudios.sarah.auth.internal.infrastructure; // Infrastructure layer of the auth module.

import com.oreostudios.sarah.auth.internal.domain.AccountStore;
import com.oreostudios.sarah.auth.internal.domain.UserAccount;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** JPA-backed {@link AccountStore}. */
@Repository
public class JpaAccountStore implements AccountStore {

    private final JpaUserAccountRepository repo;

    public JpaAccountStore(JpaUserAccountRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return repo.findByEmailIgnoreCase(email);
    }

    @Override
    public UserAccount save(UserAccount account) {
        return repo.save(account);
    }
}
