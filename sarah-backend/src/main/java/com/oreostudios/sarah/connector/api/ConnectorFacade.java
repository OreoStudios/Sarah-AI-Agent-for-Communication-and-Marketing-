package com.oreostudios.sarah.connector.api; // Public API package of the connector module.

import com.oreostudios.sarah.shared.Platform; // Platform enum (shared).

import java.util.List; // Return type.
import java.util.Optional; // Optional channel id.
import java.util.UUID; // Identifiers.

/** Public port for managing channels and dispatching publish operations. */
public interface ConnectorFacade {

    /** @return all configured channels */
    List<ChannelView> listChannels();

    /**
     * Configures a new channel and validates its credentials.
     *
     * @param command channel configuration
     * @param actor   who configured it
     * @return the new channel
     */
    ChannelView createChannel(CreateChannelCommand command, String actor);

    /**
     * Sets a channel's lifecycle status.
     *
     * @param id     channel id
     * @param status new status
     * @param actor  who performed the change
     * @return the updated channel
     */
    ChannelView setStatus(UUID id, ConnectorStatus status, String actor);

    /**
     * Publishes content to a channel. Reports accurately when a channel is not connected or the
     * platform is unsupported — never fakes success.
     *
     * @param accountId channel id
     * @param command   the content to publish
     * @return the publish outcome
     */
    PublishResult publish(UUID accountId, PublishCommand command);

    /**
     * @param type the platform
     * @return the id of a connected channel for that platform, if any
     */
    Optional<UUID> findConnectedChannelId(Platform type);

    /**
     * Adapts raw content for a channel without publishing.
     *
     * @param accountId channel id
     * @param raw       raw content
     * @param language  content language code
     * @return the prepared content
     */
    String prepare(UUID accountId, String raw, String language);
}
