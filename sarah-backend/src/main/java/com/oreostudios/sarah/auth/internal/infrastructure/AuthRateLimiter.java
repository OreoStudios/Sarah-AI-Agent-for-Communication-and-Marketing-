package com.oreostudios.sarah.auth.internal.infrastructure; // Infrastructure layer of the auth module.

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter keyed by an arbitrary string (email, IP, challenge).
 *
 * <p>Single-instance only — behind multiple instances, back this with Redis. It is a first line of
 * defence; OTP expiry, attempt caps and resend cooldowns are enforced independently in the domain.
 */
@Component
public class AuthRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

    /**
     * Records an event for {@code key} and returns whether it is within {@code maxPerWindow}.
     *
     * @return true if allowed, false if the limit is exceeded
     */
    public boolean tryAcquire(String key, int maxPerWindow) {
        Instant now = Instant.now();
        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            Instant cutoff = now.minus(WINDOW);
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
            }
            if (deque.size() >= maxPerWindow) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
