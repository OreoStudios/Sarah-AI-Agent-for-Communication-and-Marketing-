package com.oreostudios.sarah.connector.internal.infrastructure;

import com.oreostudios.sarah.connector.api.SecretVault;
import com.oreostudios.sarah.config.SarahProperties;
import org.springframework.stereotype.Component;

/** Uses the existing AES-256-GCM connector cipher and requires a persistent production key. */
@Component
public class SecretVaultAdapter implements SecretVault {
    private final TokenCipher cipher;
    private final SarahProperties config;
    public SecretVaultAdapter(TokenCipher cipher, SarahProperties config) {
        this.cipher = cipher;
        this.config = config;
    }
    private void requireKey() {
        String key = config.getSecurity().getTokenEncryptionKey();
        if (key == null || key.isBlank())
            throw new IllegalStateException("Configure a persistent SARAH_TOKEN_KEY before connecting GitHub");
    }
    @Override public String seal(String secret) {
        requireKey();
        return cipher.encrypt(secret);
    }
    @Override public String open(String ciphertext) {
        requireKey();
        return cipher.decrypt(ciphertext);
    }
}
