package com.oreostudios.sarah.auth.internal.application; // Application layer of the auth module.

import com.oreostudios.sarah.auth.internal.domain.AuthChallenge;
import com.oreostudios.sarah.auth.internal.domain.ChallengeStore;
import com.oreostudios.sarah.auth.internal.domain.EmailDeliveryService;
import com.oreostudios.sarah.auth.internal.domain.EmailDeliveryService.EmailDeliveryException;
import com.oreostudios.sarah.auth.internal.domain.Emails;
import com.oreostudios.sarah.auth.internal.infrastructure.AuthRateLimiter;
import com.oreostudios.sarah.auth.internal.infrastructure.OtpCodec;
import com.oreostudios.sarah.config.SarahProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Issues, resends and verifies one-time codes. Codes are generated securely, delivered by email,
 * and stored only as HMACs. Expiry (5 min), single-use, attempt caps, resend cooldowns and
 * issue rate limits are all enforced here — deterministically, never by the LLM.
 */
@Service
public class OtpService {

    public enum IssueOutcome { SENT, RATE_LIMITED, INVALID_EMAIL, DELIVERY_FAILED }
    public enum VerifyOutcome { SUCCESS, INVALID, EXPIRED, LOCKED, NO_CHALLENGE }

    public record IssueResult(IssueOutcome outcome, int ttlSeconds, boolean liveDelivery) { }
    public record VerifyResult(VerifyOutcome outcome, int attemptsRemaining) { }

    private final ChallengeStore store;
    private final OtpCodec codec;
    private final EmailDeliveryService emailDelivery;
    private final AuthRateLimiter rateLimiter;
    private final SarahProperties.Auth cfg;

    public OtpService(ChallengeStore store, OtpCodec codec, EmailDeliveryService emailDelivery,
                      AuthRateLimiter rateLimiter, SarahProperties props) {
        this.store = store;
        this.codec = codec;
        this.emailDelivery = emailDelivery;
        this.rateLimiter = rateLimiter;
        this.cfg = props.getAuth();
    }

    /** Issues a brand-new code for an email (replacing any prior challenge). */
    @Transactional
    public IssueResult requestCode(String rawEmail, String ip) {
        Optional<String> normalized = Emails.normalize(rawEmail);
        if (normalized.isEmpty()) {
            return new IssueResult(IssueOutcome.INVALID_EMAIL, 0, false);
        }
        String addr = normalized.get();
        if (!rateLimiter.tryAcquire("email:" + addr, cfg.getMaxCodesPerEmailPerHour())
                || (ip != null && !rateLimiter.tryAcquire("ip:" + ip, cfg.getMaxCodesPerIpPerHour()))) {
            return new IssueResult(IssueOutcome.RATE_LIMITED, 0, emailDelivery.live());
        }

        String code = codec.generateCode();
        // Deliver FIRST — if it fails we never persist a "sent" challenge (no pretending).
        try {
            emailDelivery.sendVerificationCode(addr, code);
        } catch (EmailDeliveryException e) {
            return new IssueResult(IssueOutcome.DELIVERY_FAILED, 0, emailDelivery.live());
        }

        Instant expiresAt = Instant.now().plusSeconds(cfg.getOtpTtlSeconds());
        store.deleteByEmail(addr); // One live challenge per email.
        store.save(new AuthChallenge(addr, codec.hmac(addr, code), cfg.getMaxAttempts(), expiresAt, ip));
        return new IssueResult(IssueOutcome.SENT, cfg.getOtpTtlSeconds(), emailDelivery.live());
    }

    /** Resends a code, honouring the cooldown. Falls back to a fresh issue if no challenge exists. */
    @Transactional
    public IssueResult resendCode(String rawEmail, String ip) {
        Optional<String> normalized = Emails.normalize(rawEmail);
        if (normalized.isEmpty()) {
            return new IssueResult(IssueOutcome.INVALID_EMAIL, 0, false);
        }
        String addr = normalized.get();
        AuthChallenge existing = store.findLatestByEmail(addr).orElse(null);
        Instant now = Instant.now();
        if (existing == null || existing.isConsumed()) {
            return requestCode(addr, ip);
        }
        if (!existing.canResend(now, cfg.getResendCooldownSeconds())
                || !rateLimiter.tryAcquire("email:" + addr, cfg.getMaxCodesPerEmailPerHour())) {
            return new IssueResult(IssueOutcome.RATE_LIMITED, 0, emailDelivery.live());
        }
        String code = codec.generateCode();
        try {
            emailDelivery.sendVerificationCode(addr, code);
        } catch (EmailDeliveryException e) {
            return new IssueResult(IssueOutcome.DELIVERY_FAILED, 0, emailDelivery.live());
        }
        existing.reissue(codec.hmac(addr, code), now.plusSeconds(cfg.getOtpTtlSeconds()), now);
        store.save(existing);
        return new IssueResult(IssueOutcome.SENT, cfg.getOtpTtlSeconds(), emailDelivery.live());
    }

    /** Verifies a submitted code against the active challenge for the email. */
    @Transactional
    public VerifyResult verifyCode(String rawEmail, String code) {
        Optional<String> normalized = Emails.normalize(rawEmail);
        if (normalized.isEmpty()) {
            return new VerifyResult(VerifyOutcome.NO_CHALLENGE, 0);
        }
        String addr = normalized.get();
        AuthChallenge ch = store.findLatestByEmail(addr).orElse(null);
        Instant now = Instant.now();
        if (ch == null) {
            return new VerifyResult(VerifyOutcome.NO_CHALLENGE, 0);
        }
        if (ch.isLocked()) {
            return new VerifyResult(VerifyOutcome.LOCKED, 0);
        }
        if (ch.isConsumed()) {
            // Single-use already spent — treat replay as an invalid attempt.
            return new VerifyResult(VerifyOutcome.INVALID, 0);
        }
        if (ch.isExpired(now)) {
            return new VerifyResult(VerifyOutcome.EXPIRED, 0);
        }
        if (code != null && code.length() == 6 && codec.matches(addr, code, ch.getCodeHmac())) {
            ch.consume();
            store.save(ch);
            return new VerifyResult(VerifyOutcome.SUCCESS, ch.attemptsRemaining());
        }
        ch.registerFailure();
        store.save(ch);
        return ch.isLocked()
                ? new VerifyResult(VerifyOutcome.LOCKED, 0)
                : new VerifyResult(VerifyOutcome.INVALID, ch.attemptsRemaining());
    }
}
