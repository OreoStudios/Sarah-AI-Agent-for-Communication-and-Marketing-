package com.oreostudios.sarah.auth.internal.application; // Same package as the unit under test.

import com.oreostudios.sarah.audit.api.AuditEntryView;
import com.oreostudios.sarah.audit.api.AuditFacade;
import com.oreostudios.sarah.auth.internal.domain.AccountStore;
import com.oreostudios.sarah.auth.internal.domain.AuthChallenge;
import com.oreostudios.sarah.auth.internal.domain.AuthState;
import com.oreostudios.sarah.auth.internal.domain.ChallengeStore;
import com.oreostudios.sarah.auth.internal.domain.EmailDeliveryService;
import com.oreostudios.sarah.auth.internal.domain.UserAccount;
import com.oreostudios.sarah.auth.internal.infrastructure.AuthRateLimiter;
import com.oreostudios.sarah.auth.internal.infrastructure.OtpCodec;
import com.oreostudios.sarah.config.SarahProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Drives the whole conversational flow with a mock servlet request/response and in-memory fakes. */
class ConversationalAuthServiceTest {

    static class FakeChallengeStore implements ChallengeStore {
        final Map<String, AuthChallenge> m = new HashMap<>();
        public Optional<AuthChallenge> findLatestByEmail(String e) { return Optional.ofNullable(m.get(e)); }
        public AuthChallenge save(AuthChallenge c) { m.put(c.getEmail(), c); return c; }
        public void deleteByEmail(String e) { m.remove(e); }
    }

    static class FakeAccountStore implements AccountStore {
        final Map<String, UserAccount> m = new HashMap<>();
        public Optional<UserAccount> findByEmail(String e) { return Optional.ofNullable(m.get(e)); }
        public UserAccount save(UserAccount a) { m.put(a.getEmail(), a); return a; }
    }

    static class CapturingEmail implements EmailDeliveryService {
        String lastCode;
        public void sendVerificationCode(String to, String code) { this.lastCode = code; }
        public boolean live() { return false; }
    }

    private static final AuditFacade NOOP_AUDIT = new AuditFacade() {
        public void record(String a, String ac, String t, String i, String d) { }
        public List<AuditEntryView> recent(int p, int s) { return List.of(); }
    };

    private final CapturingEmail email = new CapturingEmail();
    private final FakeAccountStore accounts = new FakeAccountStore();
    private final ConversationalAuthService service = build();

    private ConversationalAuthService build() {
        SarahProperties p = new SarahProperties();
        p.getAuth().setHmacSecret("conv-test-secret");
        OtpService otp = new OtpService(new FakeChallengeStore(), new OtpCodec(p), email, new AuthRateLimiter(), p);
        UserAccountService accountService = new UserAccountService(accounts, NOOP_AUDIT);
        SessionService session = new SessionService(new HttpSessionSecurityContextRepository());
        return new ConversationalAuthService(otp, accountService, session);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void fullRegistrationFlowThroughConversation() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        assertThat(service.handle(req, res, "hi there", "ip").state()).isEqualTo(AuthState.EMAIL_PENDING);

        var sent = service.handle(req, res, "I want to register with john@example.com", "ip");
        assertThat(sent.state()).isEqualTo(AuthState.CODE_SENT);
        assertThat(sent.email()).isEqualTo("john@example.com");
        assertThat(email.lastCode).hasSize(6);

        var verified = service.handle(req, res, "my code is " + email.lastCode, "ip");
        assertThat(verified.state()).isEqualTo(AuthState.AUTHENTICATED);
        assertThat(verified.authenticated()).isTrue();
        assertThat(accounts.findByEmail("john@example.com")).isPresent();
    }

    @Test
    void existingAccountUsesTheSameFlow() {
        accounts.save(new UserAccount("jane@example.com")); // Pre-existing account.
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        service.handle(req, res, "log me in with jane@example.com", "ip");
        var verified = service.handle(req, res, email.lastCode, "ip");
        assertThat(verified.authenticated()).isTrue();
        assertThat(verified.email()).isEqualTo("jane@example.com");
    }

    @Test
    void wrongCodeDoesNotAuthenticate() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        service.handle(req, res, "register bob@example.com", "ip");
        var r = service.handle(req, res, "000000", "ip");
        assertThat(r.authenticated()).isFalse();
        assertThat(r.state()).isEqualTo(AuthState.CODE_SENT);
    }

    @Test
    void conversationalLogoutEndsTheSession() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        service.handle(req, res, "register sam@example.com", "ip");
        service.handle(req, res, email.lastCode, "ip");
        assertThat(service.currentState(req).authenticated()).isTrue();

        var out = service.handle(req, res, "Sarah, log me out", "ip");
        assertThat(out.authenticated()).isFalse();
        assertThat(out.state()).isEqualTo(AuthState.GUEST);
    }

    @Test
    void sixDigitsAreNotTreatedAsCodeWithoutAChallenge() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        // No pending challenge: a bare 6-digit message must not be a verification attempt.
        assertThat(service.handle(req, res, "123456", "ip").state()).isEqualTo(AuthState.EMAIL_PENDING);
    }
}
