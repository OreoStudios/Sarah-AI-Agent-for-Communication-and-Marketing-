package com.oreostudios.sarah.auth.internal.infrastructure.email; // Infrastructure layer of the auth module.

import com.oreostudios.sarah.auth.internal.domain.EmailDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Development adapter (default). It does NOT send a real email and — importantly — never logs the
 * code. For real local delivery, run an SMTP server (e.g. Mailhog) and set
 * {@code sarah.auth.email.provider=smtp}. Automated tests substitute a capturing adapter to read
 * the code without it ever touching logs or the API.
 */
@Component
@ConditionalOnProperty(name = "sarah.auth.email.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailDeliveryService implements EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(LogEmailDeliveryService.class);

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        // Deliberately does not log the code. Dev-only placeholder for real SMTP.
        log.info("[DEV email adapter] A verification code would be delivered to {}. "
                + "Configure sarah.auth.email.provider=smtp (+ spring.mail.*) for real delivery.", toEmail);
    }

    @Override
    public boolean live() {
        return false;
    }
}
