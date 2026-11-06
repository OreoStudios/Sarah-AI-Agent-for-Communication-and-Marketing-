package com.oreostudios.sarah.auth.internal.infrastructure.email; // Infrastructure layer of the auth module.

import com.oreostudios.sarah.auth.internal.domain.EmailDeliveryService;
import com.oreostudios.sarah.config.SarahProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Real SMTP delivery via Spring's {@link JavaMailSender}. Active when
 * {@code sarah.auth.email.provider=smtp} (requires {@code spring.mail.*}). On failure it throws,
 * so the caller never records the code as "sent".
 */
@Component
@ConditionalOnProperty(name = "sarah.auth.email.provider", havingValue = "smtp")
public class SmtpEmailDeliveryService implements EmailDeliveryService {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailDeliveryService(JavaMailSender mailSender, SarahProperties props) {
        this.mailSender = mailSender;
        this.from = props.getAuth().getEmail().getFrom();
    }

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(toEmail);
        msg.setSubject("Your Sarah verification code");
        msg.setText("""
                Hi,

                Your Sarah verification code is: %s

                It expires in 5 minutes. If you didn't request this, you can ignore this email.

                — Sarah, Oreo Studios""".formatted(code));
        try {
            mailSender.send(msg);
        } catch (MailException e) {
            throw new EmailDeliveryException("SMTP delivery failed for " + toEmail, e);
        }
    }

    @Override
    public boolean live() {
        return true;
    }
}
