package com.oreostudios.sarah.connector.internal.infrastructure; // Infrastructure layer of the connector module.

import com.oreostudios.sarah.config.SarahProperties; // Config (OPEN module).
import org.slf4j.Logger; // Logging.
import org.slf4j.LoggerFactory; // Logger factory.
import org.springframework.stereotype.Component; // Spring component.

import javax.crypto.Cipher; // AES cipher.
import javax.crypto.spec.GCMParameterSpec; // GCM parameters.
import javax.crypto.spec.SecretKeySpec; // Key spec.
import java.nio.charset.StandardCharsets; // UTF-8.
import java.security.SecureRandom; // IV generation.
import java.util.Arrays; // Array slicing.
import java.util.Base64; // Base64 encoding.

/**
 * AES-256-GCM encryption for connector credentials at rest. The key comes from configuration /
 * secret management ({@code sarah.security.token-encryption-key}), never hardcoded. A random key
 * is generated for dev if none is provided (tokens then do not survive restarts — dev only).
 */
@Component // Shared encryption helper.
public class TokenCipher {

    private static final Logger log = LoggerFactory.getLogger(TokenCipher.class); // Logger.
    private static final int GCM_TAG_BITS = 128; // GCM tag length.
    private static final int IV_BYTES = 12; // GCM IV length.

    private final SecretKeySpec key; // AES key.
    private final SecureRandom random = new SecureRandom(); // IV source.

    /** @param props platform configuration (holds the base64 key) */
    public TokenCipher(SarahProperties props) {
        String configured = props.getSecurity().getTokenEncryptionKey(); // Configured key.
        byte[] keyBytes;
        if (configured != null && !configured.isBlank()) { // Use the configured key.
            keyBytes = Base64.getDecoder().decode(configured.strip());
            if (keyBytes.length != 32) { // Enforce 256-bit key.
                throw new IllegalStateException("sarah.security.token-encryption-key must be base64 of 32 bytes");
            }
        } else { // Dev fallback: ephemeral key.
            keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            log.warn("No token-encryption-key configured; generated an ephemeral dev key. "
                    + "Encrypted tokens will NOT survive restart. Set sarah.security.token-encryption-key in production.");
        }
        this.key = new SecretKeySpec(keyBytes, "AES"); // Build the AES key.
    }

    /** @return base64(iv || ciphertext), or null if input is null */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv); // Fresh IV per message.
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length); // Prepend IV.
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Token encryption failed", e);
        }
    }

    /** @return the decrypted plaintext, or null if input is null */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext);
            byte[] iv = Arrays.copyOfRange(all, 0, IV_BYTES); // Extract IV.
            byte[] ct = Arrays.copyOfRange(all, IV_BYTES, all.length); // Extract ciphertext.
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Token decryption failed", e);
        }
    }
}
