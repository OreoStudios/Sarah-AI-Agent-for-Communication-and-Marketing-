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

/** A passwordless account registered through Sarah's chat, keyed by verified email. */
@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email; // Lower-cased, unique.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.PENDING;

    @Column(nullable = false)
    private String role = "OWNER"; // Single-tenant: a verified user owns the Oreo Studios workspace.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected UserAccount() { }

    public UserAccount(String email) {
        this.email = email;
    }

    /** Marks the account verified/active and records the login. */
    public void markVerifiedLogin(Instant when) {
        if (this.verifiedAt == null) {
            this.verifiedAt = when;
        }
        if (this.status == AccountStatus.PENDING) {
            this.status = AccountStatus.ACTIVE;
        }
        this.lastLoginAt = when;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public AccountStatus getStatus() { return status; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
}
