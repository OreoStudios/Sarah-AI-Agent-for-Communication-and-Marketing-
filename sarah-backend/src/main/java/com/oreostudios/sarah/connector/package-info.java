/**
 * Connector module: capability-based integrations with marketing platforms. Discord is a live
 * connector (webhooks); the others are clearly-labelled mock adapters until their OAuth
 * credentials and platform approval are configured. All credential handling and outbound HTTP
 * (SSRF-guarded) live here — never under LLM control.
 */
@org.springframework.modulith.ApplicationModule( // Bounded module.
        displayName = "Connector",
        allowedDependencies = {"shared", "config", "audit :: api"} // Shared + config (SSRF/token) + audit.
)
package com.oreostudios.sarah.connector; // Root package of the connector module.
