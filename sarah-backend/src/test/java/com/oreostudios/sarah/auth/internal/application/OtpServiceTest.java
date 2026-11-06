package com.oreostudios.sarah.auth.internal.application; // Same package as the unit under test.

import com.oreostudios.sarah.auth.internal.application.OtpService.IssueOutcome;
import com.oreostudios.sarah.auth.internal.application.OtpService.VerifyOutcome;
import com.oreostudios.sarah.auth.internal.domain.AuthChallenge;
import com.oreostudios.sarah.auth.internal.domain.ChallengeStore;
import com.oreostudios.sarah.auth.internal.domain.EmailDeliveryService;
import com.oreostudios.sarah.auth.internal.infrastructure.AuthRateLimiter;
import com.oreostudios.sarah.auth.internal.infrastructure.OtpCodec;
import com.oreostudios.sarah.config.SarahProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the security-critical OTP scenarios without a database, using in-memory fakes:
 * correct / incorrect / expired / reused codes, attempt lockout, resend cooldown, rate limiting,
 * delivery failure, and that the plaintext code is never persisted.
 */
class OtpServiceTest {

    private static final String EMAIL = "john@example.com";
    private static final String IP = "203.0.113.5";

    /** One-challenge-per-email in-memory store. */
    static class FakeStore implements ChallengeStore {
        final Map<String, AuthChallenge> byEmail = new HashMap<>();
        public Optional<AuthChallenge> findLatestByEmail(String email) { return Optional.ofNullable(byEmail.get(email)); }
        public AuthChallenge save(AuthChallenge c) { byEmail.put(c.getEmail(), c); return c; }
        public void deleteByEmail(String email) { byEmail.remove(email); }
    }

    /** Captures the plaintext code (as a real adapter would receive it) and can simulate failure. */
    static class CapturingEmail implements EmailDeliveryService {
        String lastCode;
        boolean fail = false;
        public void sendVerificationCode(String to, String code) {
            if (fail) throw new EmailDeliveryException("simulated", null);
            this.lastCode = code;
        }
        public boolean live() { return false; }
    }

    private SarahProperties props(int maxAttempts, int perEmailPerHour) {
        SarahProperties p = new SarahProperties();
        p.getAuth().setHmacSecret("unit-test-secret");
        p.getAuth().setOtpTtlSeconds(300);
        p.getAuth().setMaxAttempts(maxAttempts);
        p.getAuth().setResendCooldownSeconds(60);
        p.getAuth().setMaxCodesPerEmailPerHour(perEmailPerHour);
        p.getAuth().setMaxCodesPerIpPerHour(100);
        return p;
    }

    private record Harness(OtpService otp, FakeStore store, CapturingEmail email, OtpCodec codec) { }

    private Harness harness(int maxAttempts, int perEmailPerHour) {
        SarahProperties p = props(maxAttempts, perEmailPerHour);
        FakeStore store = new FakeStore();
        CapturingEmail email = new CapturingEmail();
        OtpCodec codec = new OtpCodec(p);
        OtpService otp = new OtpService(store, codec, email, new AuthRateLimiter(), p);
        return new Harness(otp, store, email, codec);
    }

    @Test
    void issuesAndVerifiesCorrectCode() {
        Harness h = harness(5, 5);
        assertThat(h.otp.requestCode(EMAIL, IP).outcome()).isEqualTo(IssueOutcome.SENT);
        // The plaintext is never stored — only an HMAC.
        assertThat(h.store.byEmail.get(EMAIL).getCodeHmac()).isNotEqualTo(h.email.lastCode);

        assertThat(h.otp.verifyCode(EMAIL, h.email.lastCode).outcome()).isEqualTo(VerifyOutcome.SUCCESS);
    }

    @Test
    void rejectsIncorrectCodeAndCountsDownAttempts() {
        Harness h = harness(5, 5);
        h.otp.requestCode(EMAIL, IP);
        var r = h.otp.verifyCode(EMAIL, "000000");
        assertThat(r.outcome()).isEqualTo(VerifyOutcome.INVALID);
        assertThat(r.attemptsRemaining()).isEqualTo(4);
    }

    @Test
    void locksAfterTooManyAttempts() {
        Harness h = harness(3, 5);
        h.otp.requestCode(EMAIL, IP);
        h.otp.verifyCode(EMAIL, "000000");
        h.otp.verifyCode(EMAIL, "111111");
        var third = h.otp.verifyCode(EMAIL, "222222");
        assertThat(third.outcome()).isEqualTo(VerifyOutcome.LOCKED);
        // Even the correct code is rejected once locked.
        assertThat(h.otp.verifyCode(EMAIL, h.email.lastCode).outcome()).isEqualTo(VerifyOutcome.LOCKED);
    }

    @Test
    void codeIsSingleUse() {
        Harness h = harness(5, 5);
        h.otp.requestCode(EMAIL, IP);
        String code = h.email.lastCode;
        assertThat(h.otp.verifyCode(EMAIL, code).outcome()).isEqualTo(VerifyOutcome.SUCCESS);
        // Replaying the same (now consumed) code must fail.
        assertThat(h.otp.verifyCode(EMAIL, code).outcome()).isEqualTo(VerifyOutcome.INVALID);
    }

    @Test
    void expiredCodeIsRejected() {
        Harness h = harness(5, 5);
        // Seed a challenge that is already expired.
        String code = h.codec.generateCode();
        h.store.save(new AuthChallenge(EMAIL, h.codec.hmac(EMAIL, code), 5,
                Instant.now().minusSeconds(1), IP));
        assertThat(h.otp.verifyCode(EMAIL, code).outcome()).isEqualTo(VerifyOutcome.EXPIRED);
    }

    @Test
    void resendIsBlockedByCooldown() {
        Harness h = harness(5, 5);
        h.otp.requestCode(EMAIL, IP);
        // Immediately resending is within the 60s cooldown.
        assertThat(h.otp.resendCode(EMAIL, IP).outcome()).isEqualTo(IssueOutcome.RATE_LIMITED);
    }

    @Test
    void issueRateLimitIsEnforced() {
        Harness h = harness(5, 2); // 2 codes/email/hour
        assertThat(h.otp.requestCode(EMAIL, IP).outcome()).isEqualTo(IssueOutcome.SENT);
        assertThat(h.otp.requestCode(EMAIL, IP).outcome()).isEqualTo(IssueOutcome.SENT);
        assertThat(h.otp.requestCode(EMAIL, IP).outcome()).isEqualTo(IssueOutcome.RATE_LIMITED);
    }

    @Test
    void deliveryFailureNeverRecordsASentChallenge() {
        Harness h = harness(5, 5);
        h.email.fail = true;
        assertThat(h.otp.requestCode(EMAIL, IP).outcome()).isEqualTo(IssueOutcome.DELIVERY_FAILED);
        assertThat(h.store.byEmail).doesNotContainKey(EMAIL); // No challenge persisted.
    }

    @Test
    void invalidEmailIsRejected() {
        Harness h = harness(5, 5);
        assertThat(h.otp.requestCode("not-an-email", IP).outcome()).isEqualTo(IssueOutcome.INVALID_EMAIL);
    }
}
