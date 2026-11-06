/**
 * Application-wide configuration: strongly-typed properties and shared infrastructure beans
 * (OpenAPI metadata, base HTTP client).
 *
 * <p>Declared {@link org.springframework.modulith.ApplicationModule.Type#OPEN open} so any
 * module may inject {@link com.oreostudios.sarah.config.SarahProperties} without declaring a
 * dependency.
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN) // OPEN: config usable everywhere.
package com.oreostudios.sarah.config; // Root package of the config module.
