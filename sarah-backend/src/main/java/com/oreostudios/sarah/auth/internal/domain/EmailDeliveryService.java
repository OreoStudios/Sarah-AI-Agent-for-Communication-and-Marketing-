package com.oreostudios.sarah.auth.internal.domain; // Domain layer of the auth module.

/**
 * Port for delivering a verification code by email. Implementations MUST throw
 * {@link EmailDeliveryException} when delivery fails — the platform never pretends an email was
 * sent. The plaintext code is passed here only; it is never persisted, logged, or returned to the API.
 */
public interface EmailDeliveryService {

    /**
     * Delivers the code to the address.
     *
     * @param toEmail recipient
     * @param code    the 6-digit plaintext code (handled in-memory only)
     * @throws EmailDeliveryException if delivery could not be completed
     */
    void sendVerificationCode(String toEmail, String code);

    /** @return whether this adapter performs real external delivery (false = dev/log adapter). */
    boolean live();

    /** Thrown when an email could not be delivered. */
    class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message, Throwable cause) { super(message, cause); }
    }
}
