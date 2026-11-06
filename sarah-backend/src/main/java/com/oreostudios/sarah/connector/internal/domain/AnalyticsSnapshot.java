package com.oreostudios.sarah.connector.internal.domain; // Domain layer of the connector module.

/**
 * Engagement analytics snapshot for a publication.
 *
 * @param impressions impression count
 * @param clicks      click count
 * @param engagements engagement count
 * @param note        a note (e.g. why analytics are unavailable)
 */
public record AnalyticsSnapshot(long impressions, long clicks, long engagements, String note) {
    /** @return a zeroed snapshot carrying an explanatory note */
    public static AnalyticsSnapshot unavailable(String note) {
        return new AnalyticsSnapshot(0, 0, 0, note);
    }
}
