package com.oreostudios.sarah.shared; // Shared error representation.

/**
 * Thrown by deterministic policy / permission enforcement (autonomy limits, kill switch,
 * capability checks, illegal state transitions). Mapped to HTTP 409 Conflict.
 *
 * <p>Final permission enforcement is Java code — never delegated to the LLM.
 */
public class PolicyViolationException extends RuntimeException {
    /** @param message detail describing the violated policy */
    public PolicyViolationException(String message) {
        super(message); // Delegate to RuntimeException.
    }
}
