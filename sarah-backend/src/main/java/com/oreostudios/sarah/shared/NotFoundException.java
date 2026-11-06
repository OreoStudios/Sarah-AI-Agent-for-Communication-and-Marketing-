package com.oreostudios.sarah.shared; // Shared error representation.

/** Thrown when a requested domain entity does not exist. Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {
    /** @param message detail describing what was not found */
    public NotFoundException(String message) {
        super(message); // Delegate to RuntimeException.
    }
}
