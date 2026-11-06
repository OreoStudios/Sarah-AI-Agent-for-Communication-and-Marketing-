package com.oreostudios.sarah.connector.api; // Public API package of the connector module.

/**
 * Outcome of a publish attempt.
 *
 * @param success     whether the platform accepted the content
 * @param externalId  platform-assigned id (e.g. Discord message id), if any
 * @param externalUrl permalink, if the platform exposes one
 * @param message     human-readable detail
 * @param retriable   whether a failure is transient and safe to retry
 */
public record PublishResult(boolean success, String externalId, String externalUrl,
                            String message, boolean retriable) {

    /** @return a successful result */
    public static PublishResult ok(String externalId, String externalUrl, String message) {
        return new PublishResult(true, externalId, externalUrl, message, false);
    }

    /** @return a transient (retriable) failure */
    public static PublishResult transientFailure(String message) {
        return new PublishResult(false, null, null, message, true);
    }

    /** @return a permanent (non-retriable) failure */
    public static PublishResult permanentFailure(String message) {
        return new PublishResult(false, null, null, message, false);
    }
}
