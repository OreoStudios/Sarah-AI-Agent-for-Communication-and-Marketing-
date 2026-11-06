package com.oreostudios.sarah.connector.internal.application; // Application layer of the connector module.

import com.oreostudios.sarah.audit.api.AuditFacade; // Audit port.
import com.oreostudios.sarah.connector.api.Capability; // Capabilities.
import com.oreostudios.sarah.connector.api.ChannelView; // Channel DTO.
import com.oreostudios.sarah.connector.api.ConnectorFacade; // Implemented public port.
import com.oreostudios.sarah.connector.api.ConnectorStatus; // Status enum.
import com.oreostudios.sarah.connector.api.CreateChannelCommand; // Create command.
import com.oreostudios.sarah.connector.api.PublishCommand; // Publish command.
import com.oreostudios.sarah.connector.api.PublishResult; // Publish result.
import com.oreostudios.sarah.connector.internal.domain.ConnectorAccount; // Entity.
import com.oreostudios.sarah.connector.internal.domain.ConnectorContext; // Operation context.
import com.oreostudios.sarah.connector.internal.domain.SocialConnector; // SPI.
import com.oreostudios.sarah.connector.internal.infrastructure.ConnectorRegistry; // Connector registry.
import com.oreostudios.sarah.connector.internal.infrastructure.JpaConnectorAccountRepository; // Repository.
import com.oreostudios.sarah.connector.internal.infrastructure.TokenCipher; // Token encryption.
import com.oreostudios.sarah.shared.Platform; // Platform enum.
import com.oreostudios.sarah.shared.NotFoundException; // 404 signal.
import org.springframework.stereotype.Service; // Spring service bean.
import org.springframework.transaction.annotation.Transactional; // Transactional boundaries.

import java.util.List; // Return types.
import java.util.Optional; // Optional channel id.
import java.util.Set; // Capability set.
import java.util.UUID; // Identifiers.
import java.util.stream.Collectors; // CSV join.

/**
 * Manages connector accounts and dispatches publish operations to the right {@link SocialConnector}.
 * All credential decryption and HTTP stay here and in connectors — never exposed to the LLM.
 */
@Service // Implements the connector port.
public class ConnectorService implements ConnectorFacade {

    private final JpaConnectorAccountRepository accounts; // Persistence gateway.
    private final ConnectorRegistry registry; // Connector lookup.
    private final TokenCipher cipher; // Credential encryption.
    private final AuditFacade audit; // Audit port.

    ConnectorService(JpaConnectorAccountRepository accounts, ConnectorRegistry registry,
                     TokenCipher cipher, AuditFacade audit) {
        this.accounts = accounts;
        this.registry = registry;
        this.cipher = cipher;
        this.audit = audit;
    }

    @Override
    @Transactional
    public ChannelView createChannel(CreateChannelCommand cmd, String actor) {
        SocialConnector connector = registry.forPlatform(cmd.type()) // Resolve the connector.
                .orElseThrow(() -> new NotFoundException("No connector available for " + cmd.type()));

        String encrypted = cipher.encrypt(cmd.secret()); // Encrypt the credential.
        ConnectorStatus status;
        try {
            connector.validateCredentials(new ConnectorContext(cmd.name(), cmd.config(), cmd.secret())); // Validate.
            // Only a live connector with valid credentials is truly CONNECTED. Mock adapters report
            // NEEDS_SETUP so real credentials/approval are surfaced.
            status = connector.live() ? ConnectorStatus.CONNECTED : ConnectorStatus.NEEDS_SETUP;
        } catch (RuntimeException e) {
            status = ConnectorStatus.NEEDS_SETUP; // Invalid/missing credentials.
        }
        String caps = connector.capabilities().stream().map(Enum::name).collect(Collectors.joining(",")); // Snapshot.
        ConnectorAccount acct = accounts.save(new ConnectorAccount(cmd.type(), cmd.name(), cmd.config(),
                encrypted, caps, status)); // Persist.
        audit.record(actor, "CONNECTOR_CREATED", "connector", acct.getId().toString(),
                "type=" + cmd.type() + " status=" + status + " live=" + connector.live());
        return toView(acct, connector);
    }

    @Override
    @Transactional
    public ChannelView setStatus(UUID id, ConnectorStatus status, String actor) {
        ConnectorAccount acct = require(id); // Load.
        acct.updateStatus(status); // Apply.
        audit.record(actor, "CONNECTOR_STATUS", "connector", id.toString(), "status=" + status);
        return toView(acct, registry.forPlatform(acct.getType()).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChannelView> listChannels() {
        return accounts.findAll().stream()
                .map(a -> toView(a, registry.forPlatform(a.getType()).orElse(null)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findConnectedChannelId(Platform type) {
        return accounts.findFirstByTypeAndStatus(type, ConnectorStatus.CONNECTED).map(ConnectorAccount::getId);
    }

    @Override
    @Transactional
    public PublishResult publish(UUID accountId, PublishCommand request) {
        ConnectorAccount acct = require(accountId); // Load.
        SocialConnector connector = registry.forPlatform(acct.getType()).orElse(null); // Resolve.
        if (connector == null) { // Unsupported platform.
            return PublishResult.permanentFailure("No connector available for " + acct.getType());
        }
        if (acct.getStatus() != ConnectorStatus.CONNECTED) { // Report, never fake.
            return PublishResult.permanentFailure(
                    "Channel '" + acct.getName() + "' (" + acct.getType().displayName()
                    + ") is " + acct.getStatus() + " — cannot publish until connected.");
        }
        ConnectorContext ctx = context(acct); // Decrypt credentials.
        try {
            connector.validateCredentials(ctx); // Re-validate before use.
        } catch (RuntimeException e) {
            return PublishResult.permanentFailure("Credential validation failed: " + e.getMessage());
        }
        return connector.publish(ctx, request); // Dispatch.
    }

    @Override
    @Transactional(readOnly = true)
    public String prepare(UUID accountId, String raw, String language) {
        ConnectorAccount acct = require(accountId); // Load.
        SocialConnector connector = registry.forPlatform(acct.getType())
                .orElseThrow(() -> new NotFoundException("No connector for " + acct.getType()));
        return connector.prepareContent(raw, language);
    }

    private ConnectorContext context(ConnectorAccount acct) {
        return new ConnectorContext(acct.getName(), acct.getConfig(), cipher.decrypt(acct.getEncryptedToken())); // Decrypt.
    }

    private ChannelView toView(ConnectorAccount a, SocialConnector connector) {
        boolean live = connector != null && connector.live();
        Set<Capability> caps = connector != null ? connector.capabilities() : Set.of();
        String hint = live ? null
                : "Real publishing requires platform credentials/app approval for " + a.getType().displayName();
        return new ChannelView(a.getId(), a.getType(), a.getName(), a.getStatus(), live, caps, hint);
    }

    private ConnectorAccount require(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Connector not found: " + id));
    }
}
