package com.oreostudios.sarah.connector.internal.infrastructure; // Same package as the unit under test.

import com.oreostudios.sarah.config.SarahProperties; // Config holding the allow-list.
import com.oreostudios.sarah.shared.PolicyViolationException; // Expected failure type.
import org.junit.jupiter.api.Test; // JUnit 5.

import java.util.List; // Allow-list.

import static org.assertj.core.api.Assertions.assertThatThrownBy; // Exception assertions.

class OutboundHttpGuardTest {

    private OutboundHttpGuard guard() {
        SarahProperties props = new SarahProperties();
        props.getHttp().setOutboundAllowedHosts(List.of("discord.com"));
        return new OutboundHttpGuard(props);
    }

    @Test
    void rejectsNonHttps() {
        assertThatThrownBy(() -> guard().validate("ftp://discord.com/x"))
                .isInstanceOf(PolicyViolationException.class);
    }

    @Test
    void rejectsDisallowedHost() {
        assertThatThrownBy(() -> guard().validate("https://evil.example.com/webhook"))
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void rejectsMalformedUrl() {
        assertThatThrownBy(() -> guard().validate("https://"))
                .isInstanceOf(PolicyViolationException.class);
    }

    @Test
    void rejectsSubdomainSpoofing() {
        assertThatThrownBy(() -> guard().validate("https://discord.com.evil.example.com/x"))
                .isInstanceOf(PolicyViolationException.class);
    }
}
