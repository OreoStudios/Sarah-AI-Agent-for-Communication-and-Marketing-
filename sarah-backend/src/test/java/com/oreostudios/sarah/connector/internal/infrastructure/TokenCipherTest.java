package com.oreostudios.sarah.connector.internal.infrastructure; // Same package as the unit under test.

import com.oreostudios.sarah.config.SarahProperties; // Config holding the key.
import org.junit.jupiter.api.Test; // JUnit 5.

import java.util.Base64; // Key encoding.

import static org.assertj.core.api.Assertions.assertThat; // Fluent assertions.

class TokenCipherTest {

    private TokenCipher cipherWithFixedKey() {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) i;
        SarahProperties props = new SarahProperties();
        props.getSecurity().setTokenEncryptionKey(Base64.getEncoder().encodeToString(key));
        return new TokenCipher(props);
    }

    @Test
    void roundTripsSecret() {
        TokenCipher cipher = cipherWithFixedKey();
        String secret = "https://discord.com/api/webhooks/123/abc";
        String encrypted = cipher.encrypt(secret);

        assertThat(encrypted).isNotEqualTo(secret);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(secret);
    }

    @Test
    void producesDifferentCiphertextEachTime() {
        TokenCipher cipher = cipherWithFixedKey();
        assertThat(cipher.encrypt("same")).isNotEqualTo(cipher.encrypt("same"));
    }

    @Test
    void handlesNull() {
        TokenCipher cipher = cipherWithFixedKey();
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
    }
}
