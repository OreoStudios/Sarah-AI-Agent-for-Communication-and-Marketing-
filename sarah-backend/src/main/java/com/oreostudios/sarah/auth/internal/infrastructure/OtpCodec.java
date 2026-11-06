package com.oreostudios.sarah.auth.internal.infrastructure; // Infrastructure layer of the auth module.

import com.oreostudios.sarah.config.SarahProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Generates cryptographically secure 6-digit codes and binds them to an email via HMAC-SHA256.
 * Only the HMAC is ever stored; verification recomputes and compares in constant time.
 */
@Component
public class OtpCodec {

    private static final Logger log = LoggerFactory.getLogger(OtpCodec.class);

    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public OtpCodec(SarahProperties props) {
        String secret = props.getAuth().getHmacSecret();
        if (secret == null || secret.isBlank()) {
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);
            this.key = generated;
            log.warn("No sarah.auth.hmac-secret configured; using an ephemeral key. "
                    + "Verification codes will not survive a restart. Set a stable secret in production.");
        } else {
            this.key = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    /** @return a fresh, uniformly random 6-digit code. */
    public String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /** @return the HMAC that binds a code to an email (lower-cased). */
    public String hmac(String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal((email.toLowerCase(Locale.ROOT) + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    /** Constant-time verification of a submitted code against a stored HMAC. */
    public boolean matches(String email, String code, String storedHmac) {
        String computed = hmac(email, code);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                storedHmac.getBytes(StandardCharsets.UTF_8));
    }
}
