package com.oreostudios.sarah.connector.api; // Public API package of the connector module.

/** Lifecycle/health state of a connector account. */
public enum ConnectorStatus {
    CONNECTED, // Credentials present and validated; ready within its capabilities.
    DISCONNECTED, // Explicitly disconnected by the owner.
    NEEDS_SETUP, // Configured but missing credentials/approval; cannot act yet.
    UNSUPPORTED // The platform does not offer the required API/permission for this action.
}
