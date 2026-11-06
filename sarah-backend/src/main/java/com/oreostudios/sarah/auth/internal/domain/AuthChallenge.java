package com.oreostudios.sarah.auth.internal.domain; // Domain layer of the auth module.

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A single email verification challenge. Stores only an HMAC of the code — never the plaintext.
 * Enforces expiry, single-use, and a maximum attempt count (deterministically).
 */
@Entity
@Table(name = "auth_challenge")
public class AuthChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "code_hmac", nullable = false)
    private String codeHmac; // HMAC-SHA256(secret, email:code).

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeState state = ChallengeState.SENT;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "resend_count", nullable = false)
    private int resendCount = 0;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt = Instant.now();

    @Column(name = "request_ip")
    private String requestIp;

    protected AuthChallenge() { }

    public AuthChallenge(String email, String codeHmac, int maxAttempts, Instant expiresAt, String requestIp) {
        this.email = email;
        this.codeHmac = codeHmac;
        this.maxAttempts = maxAttempts;
        this.expiresAt = expiresAt;
        this.requestIp = requestIp;
        this.lastSentAt = Instant.now();
    }

    public boolean isExpired(Instant now) { return now.isAfter(expiresAt); }
    public boolean isConsumed() { return state == ChallengeState.CONSUMED; }
    public boolean isLocked() { return state == ChallengeState.LOCKED; }

    /** Whether a verification attempt can be made right now. */
    public boolean verifiable(Instant now) { return state == ChallengeState.SENT && !isExpired(now); }

    /** Records a failed attempt; locks the challenge once the attempt ceiling is reached. */
    public void registerFailure() {
        this.attempts += 1;
        if (this.attempts >= this.maxAttempts) {
            this.state = ChallengeState.LOCKED;
        }
    }

    /** Marks the challenge successfully spent (single-use). */
    public void consume() { this.state = ChallengeState.CONSUMED; }

    /** Issues a fresh code into the same challenge, resetting attempts. */
    public void reissue(String codeHmac, Instant expiresAt, Instant now) {
        this.codeHmac = codeHmac;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.state = ChallengeState.SENT;
        this.resendCount += 1;
        this.lastSentAt = now;
    }

    /** Whether a resend is allowed (cooldown elapsed and not already consumed). */
    public boolean canResend(Instant now, int cooldownSeconds) {
        return state != ChallengeState.CONSUMED && now.isAfter(lastSentAt.plusSeconds(cooldownSeconds));
    }

    public int attemptsRemaining() { return Math.max(0, maxAttempts - attempts); }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getCodeHmac() { return codeHmac; }
    public ChallengeState getState() { return state; }
    public int getAttempts() { return attempts; }
    public int getResendCount() { return resendCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getLastSentAt() { return lastSentAt; }
    public String getRequestIp() { return requestIp; }
}
