package com.oreostudios.sarah.auth.internal.application; // Application layer of the auth module.

import com.oreostudios.sarah.audit.api.AuditFacade;
import com.oreostudios.sarah.auth.internal.domain.AccountStore;
import com.oreostudios.sarah.auth.internal.domain.Emails;
import com.oreostudios.sarah.auth.internal.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Creates and verifies passwordless accounts keyed by email. */
@Service
public class UserAccountService {

    private final AccountStore store;
    private final AuditFacade audit;

    public UserAccountService(AccountStore store, AuditFacade audit) {
        this.store = store;
        this.audit = audit;
    }

    /**
     * Finds the account for an email, or creates a PENDING one. The same flow serves new and
     * existing accounts — so a registration attempt for an existing email never reveals which it is
     * (no enumeration) and never creates a duplicate.
     */
    @Transactional
    public UserAccount findOrCreatePending(String email) {
        String addr = Emails.normalize(email).orElseThrow(() -> new IllegalArgumentException("Invalid email"));
        return store.findByEmail(addr).orElseGet(() -> {
            UserAccount created = store.save(new UserAccount(addr));
            audit.record(addr, "ACCOUNT_REGISTERED", "account", String.valueOf(created.getId()), "pending verification");
            return created;
        });
    }

    /** Marks an account verified and records the login. Called only after a successful OTP check. */
    @Transactional
    public UserAccount recordVerifiedLogin(String email) {
        String addr = Emails.normalize(email).orElseThrow(() -> new IllegalArgumentException("Invalid email"));
        UserAccount account = store.findByEmail(addr).orElseGet(() -> store.save(new UserAccount(addr)));
        account.markVerifiedLogin(Instant.now());
        account = store.save(account);
        audit.record(addr, "AUTH_VERIFIED", "account", String.valueOf(account.getId()), null);
        return account;
    }
}
