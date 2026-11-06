package com.oreostudios.sarah.connector.api; // Public API package of the connector module.

/** Operations a connector may support. Reported honestly per integration. */
public enum Capability {
    VALIDATE_CREDENTIALS, // Can verify credentials.
    PREPARE_CONTENT, // Can adapt content to the platform.
    PUBLISH, // Can publish content.
    SCHEDULE, // Can schedule content.
    FETCH_STATUS, // Can report publication status.
    FETCH_ANALYTICS // Can fetch engagement analytics.
}
