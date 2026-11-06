package com.oreostudios.sarah.connector.internal.infrastructure; // Infrastructure layer of the connector module.

import com.oreostudios.sarah.config.SarahProperties; // Config (OPEN module).
import com.oreostudios.sarah.shared.PolicyViolationException; // Policy-violation signal.
import org.springframework.stereotype.Component; // Spring component.

import java.net.InetAddress; // Address resolution.
import java.net.URI; // URL parsing.
import java.net.UnknownHostException; // Resolution failure.
import java.util.Locale; // Lower-casing.
import java.util.Set; // Allow-list.

/**
 * SSRF protection for any outbound URL the platform posts to.
 *
 * <p>Enforces (deterministically, in Java — never the LLM): https only; host on the configured
 * allow-list; resolved address must not be loopback / link-local / site-local / private.
 */
@Component // Shared outbound-URL guard.
public class OutboundHttpGuard {

    private final Set<String> allowedHosts; // Lower-cased allow-list.

    /** @param props platform configuration (holds the allow-list) */
    public OutboundHttpGuard(SarahProperties props) {
        this.allowedHosts = Set.copyOf(props.getHttp().getOutboundAllowedHosts().stream()
                .map(h -> h.toLowerCase(Locale.ROOT).strip())
                .toList());
    }

    /**
     * Validates the URL and returns a normalised {@link URI}, or throws.
     *
     * @param url the candidate URL
     * @return the validated URI
     * @throws PolicyViolationException if the URL is not permitted
     */
    public URI validate(String url) {
        URI uri;
        try {
            uri = URI.create(url.strip()); // Parse.
        } catch (IllegalArgumentException e) {
            throw new PolicyViolationException("Malformed outbound URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase("https")) { // https only.
            throw new PolicyViolationException("Outbound requests must use https");
        }
        String host = uri.getHost();
        if (host == null) { // Host required.
            throw new PolicyViolationException("Outbound URL has no host");
        }
        String lowerHost = host.toLowerCase(Locale.ROOT);
        boolean allowed = allowedHosts.stream().anyMatch(h -> lowerHost.equals(h) || lowerHost.endsWith("." + h)); // Allow-list.
        if (!allowed) {
            throw new PolicyViolationException("Host not allowed for outbound requests: " + host);
        }
        assertNotInternalAddress(host); // Block non-routable targets.
        return uri;
    }

    private void assertNotInternalAddress(String host) {
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) { // All resolved addresses.
                if (addr.isLoopbackAddress() || addr.isAnyLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                        || addr.isMulticastAddress()) { // Reject internal ranges.
                    throw new PolicyViolationException("Outbound host resolves to a non-routable address");
                }
            }
        } catch (UnknownHostException e) {
            throw new PolicyViolationException("Outbound host could not be resolved");
        }
    }
}
