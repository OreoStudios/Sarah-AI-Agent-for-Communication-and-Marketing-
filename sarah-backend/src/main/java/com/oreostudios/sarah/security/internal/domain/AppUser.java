package com.oreostudios.sarah.security.internal.domain; // Domain layer of the security module.

import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.EnumType; // Enum persistence selector.
import jakarta.persistence.Enumerated; // Enum persistence declaration.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Creation timestamp.
import java.util.UUID; // Identifier type.

/** Application user / owner account. */
@Entity // Managed entity.
@Table(name = "app_user") // Backing table.
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // User id.

    @Column(nullable = false, unique = true)
    private String email; // Unique login email.

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // BCrypt password hash.

    @Column(name = "display_name", nullable = false)
    private String displayName; // Display name.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // Assigned role.

    @Column(nullable = false)
    private boolean enabled = true; // Whether the account is active.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Creation time.

    /** No-args constructor required by JPA. */
    protected AppUser() {
    }

    /**
     * Creates an enabled user.
     *
     * @param email        login email
     * @param passwordHash BCrypt hash
     * @param displayName  display name
     * @param role         assigned role
     */
    public AppUser(String email, String passwordHash, String displayName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
}
