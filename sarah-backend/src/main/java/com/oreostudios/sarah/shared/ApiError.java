package com.oreostudios.sarah.shared; // Shared error representation.

import java.time.Instant; // Timestamp of the error.
import java.util.List; // Field-level detail messages.

/** Consistent error response body for all API endpoints. */
public record ApiError(
        Instant timestamp, // When the error occurred.
        int status, // HTTP status code.
        String error, // HTTP reason phrase.
        String message, // Human-readable message.
        String path, // Request path that failed.
        List<String> details // Field-level validation details (may be empty).
) {
    /**
     * Factory normalising a null detail list to an empty one.
     *
     * @param status  HTTP status code
     * @param error   HTTP reason phrase
     * @param message human-readable message
     * @param path    request path
     * @param details field-level details, or null
     * @return the error body
     */
    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, path, details == null ? List.of() : details);
    }
}
