package com.oreostudios.sarah.connector.internal.domain; // Domain layer of the connector module.

/**
 * Delivery status of a previously published item.
 *
 * @param externalId platform-assigned id
 * @param state      a coarse state string (e.g. DELIVERED/MOCK/UNKNOWN)
 * @param detail     human-readable detail
 */
public record PublicationStatusInfo(String externalId, String state, String detail) {
}
