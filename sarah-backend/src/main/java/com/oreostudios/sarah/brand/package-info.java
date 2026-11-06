/**
 * Brand module: the configurable company/brand profile (name, services, tone, languages).
 * Agents read this through {@code brand :: api}; it is never hardcoded into prompts.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Brand") // Bounded module; only OPEN dependencies.
package com.oreostudios.sarah.brand; // Root package of the brand module.
