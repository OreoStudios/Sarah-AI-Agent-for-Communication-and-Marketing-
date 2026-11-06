package com.oreostudios.sarah.auth.internal.infrastructure.web; // Web adapter of the auth module.

import com.oreostudios.sarah.auth.internal.application.AuthReply;
import com.oreostudios.sarah.auth.internal.application.ConversationalAuthService;
import com.oreostudios.sarah.auth.internal.application.SessionService;
import com.oreostudios.sarah.auth.internal.domain.AuthState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conversational authentication endpoints under {@code /api/auth/**}. These are the only routes a
 * guest may reach — they grant no access to campaigns, tools, or private data. Authentication
 * messages are handled deterministically here, never by the AI orchestrator.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record ChatMessageRequest(@NotBlank String message) { }
    public record EmailRequest(@NotBlank String email) { }
    public record CodeRequest(@NotBlank String code) { }

    private final ConversationalAuthService auth;
    private final SessionService session;

    public AuthController(ConversationalAuthService auth, SessionService session) {
        this.auth = auth;
        this.session = session;
    }

    /** Returns the current authentication state (used to bootstrap/refresh the SPA). */
    @GetMapping("/session")
    public AuthReply session(HttpServletRequest request) {
        return auth.currentState(request);
    }

    /** Sarah's conversational onboarding/auth turn. */
    @PostMapping("/chat")
    public AuthReply chat(@RequestBody ChatMessageRequest req, HttpServletRequest request, HttpServletResponse response) {
        return auth.handle(request, response, req.message(), clientIp(request));
    }

    /** Direct code request (e.g. from an email field), kept consistent with the chat flow. */
    @PostMapping("/request-code")
    public AuthReply requestCode(@RequestBody EmailRequest req, HttpServletRequest request) {
        return auth.issue(request, req.email(), clientIp(request), false);
    }

    /** Verify a code against the challenge bound to the session. */
    @PostMapping("/verify-code")
    public AuthReply verifyCode(@RequestBody CodeRequest req, HttpServletRequest request, HttpServletResponse response) {
        String pending = session.pendingEmail(request);
        if (pending == null) {
            return new AuthReply("I don't have a code on file for you yet. What's your email address?",
                    AuthState.EMAIL_PENDING, null, false);
        }
        return auth.verify(request, response, pending, req.code());
    }

    /** Resend the code for the pending challenge. */
    @PostMapping("/resend-code")
    public AuthReply resendCode(HttpServletRequest request) {
        String pending = session.pendingEmail(request);
        if (pending == null) {
            return new AuthReply("Tell me your email first and I'll send a code.", AuthState.EMAIL_PENDING, null, false);
        }
        return auth.issue(request, pending, clientIp(request), true);
    }

    /** Server-side logout — invalidates the session. */
    @PostMapping("/logout")
    public AuthReply logout(HttpServletRequest request, HttpServletResponse response) {
        auth.logout(request, response);
        return new AuthReply("You've been securely signed out.", AuthState.GUEST, null, false);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
