package com.oreostudios.sarah.connector.internal.domain; // Domain layer of the connector module.

import com.oreostudios.sarah.connector.api.ConnectorStatus; // Public status enum.
import com.oreostudios.sarah.shared.Platform; // Platform enum (shared).
import jakarta.persistence.Column; // Column mapping.
import jakarta.persistence.Entity; // JPA entity marker.
import jakarta.persistence.EnumType; // Enum persistence selector.
import jakarta.persistence.Enumerated; // Enum persistence declaration.
import jakarta.persistence.GeneratedValue; // Id generation.
import jakarta.persistence.GenerationType; // Generation strategy.
import jakarta.persistence.Id; // Primary key.
import jakarta.persistence.Table; // Table binding.

import java.time.Instant; // Timestamps.
import java.util.UUID; // Identifier type.

/** A configured channel/account for a platform. The token is stored encrypted at rest. */
@Entity // Managed entity.
@Table(name = "connector_account") // Backing table.
public class ConnectorAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Auto-assigned UUID.
    private UUID id; // Channel id.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Platform type; // Platform.

    @Column(nullable = false)
    private String name; // Human label.

    @Column
    private String config; // Non-secret JSON config (nullable).

    @Column(name = "encrypted_token")
    private String encryptedToken; // AES-GCM encrypted credential (nullable).

    @Column(nullable = false)
    private String capabilities = ""; // CSV of supported capabilities (snapshot).

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectorStatus status = ConnectorStatus.NEEDS_SETUP; // Lifecycle status.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now(); // Creation time.

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now(); // Last update time.

    /** No-args constructor required by JPA. */
    protected ConnectorAccount() {
    }

    /** Creates a channel with its initial status and capability snapshot. */
    public ConnectorAccount(Platform type, String name, String config, String encryptedToken,
                            String capabilities, ConnectorStatus status) {
        this.type = type;
        this.name = name;
        this.config = config;
        this.encryptedToken = encryptedToken;
        this.capabilities = capabilities;
        this.status = status;
    }

    /** Updates the lifecycle status. */
    public void updateStatus(ConnectorStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Platform getType() { return type; }
    public String getName() { return name; }
    public String getConfig() { return config; }
    public String getEncryptedToken() { return encryptedToken; }
    public String getCapabilities() { return capabilities; }
    public ConnectorStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
