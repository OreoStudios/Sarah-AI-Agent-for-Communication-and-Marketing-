package com.oreostudios.sarah.auth.internal.application; // Application layer of the auth module.

import com.oreostudios.sarah.auth.internal.application.OtpService.IssueOutcome;
import com.oreostudios.sarah.auth.internal.application.OtpService.IssueResult;
import com.oreostudios.sarah.auth.internal.application.OtpService.VerifyResult;
import com.oreostudios.sarah.auth.internal.domain.AuthState;
import com.oreostudios.sarah.auth.internal.domain.Emails;
import com.oreostudios.sarah.auth.internal.domain.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Deterministic handler that turns onboarding/auth messages into state transitions. It runs BEFORE
 * (and entirely without) the AI orchestrator: email validation, OTP issue/verify, session creation
 * and logout are all Java. The LLM never sees codes and cannot be prompt-injected into bypassing it.
 */
@Service
public class ConversationalAuthService {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\b(\\d{6})\\b");
    private static final Pattern LOGOUT = Pattern.compile("(?i)\\b(log\\s*out|logout|sign\\s*out|log me out)\\b");
    private static final Pattern RESEND = Pattern.compile("(?i)\\b(resend|re-send|send.*again|new code|another code)\\b");

    private final OtpService otp;
    private final UserAccountService accounts;
    private final SessionService session;

    public ConversationalAuthService(OtpService otp, UserAccountService accounts, SessionService session) {
        this.otp = otp;
        this.accounts = accounts;
        this.session = session;
    }

    public AuthReply handle(HttpServletRequest request, HttpServletResponse response, String message, String ip) {
        String text = message == null ? "" : message.strip();

        // Already signed in: only logout is meaningful here.
        if (session.isAuthenticated()) {
            if (LOGOUT.matcher(text).find()) {
                session.logout(request, response);
                return AuthReply.of("Of course. You've been securely signed out. See you soon!", AuthState.GUEST, null, false);
            }
            return AuthReply.of("You're already signed in. Head into the workspace whenever you're ready.",
                    AuthState.AUTHENTICATED, session.currentEmail(), true);
        }

        if (LOGOUT.matcher(text).find()) {
            return AuthReply.of("You're not signed in yet — let's fix that. What's your email address?", AuthState.GUEST, null, false);
        }

        String pending = session.pendingEmail(request);

        // A fresh email starts (or restarts) a challenge — same flow for new and returning users.
        Optional<String> email = Emails.extract(text);
        if (email.isPresent()) {
            return issue(request, email.get(), ip, false);
        }

        // A 6-digit code is only a verification attempt when a challenge is pending.
        if (pending != null) {
            var m = SIX_DIGITS.matcher(text);
            if (m.find()) {
                return verify(request, response, pending, m.group(1));
            }
            if (RESEND.matcher(text).find()) {
                return issue(request, pending, ip, true);
            }
            return AuthReply.of("I'm still waiting on the 6-digit code I sent to " + pending
                    + ". Enter it here, or say “resend” for a new one.", AuthState.CODE_SENT, pending, false);
        }

        if (RESEND.matcher(text).find()) {
            return AuthReply.of("Tell me your email first and I'll send a code.", AuthState.EMAIL_PENDING, null, false);
        }

        // Greeting / anything else.
        return AuthReply.of("Hey! I'm Sarah, your AI marketing assistant. 👋 "
                + "Before we get started, what's your email address?", AuthState.EMAIL_PENDING, null, false);
    }

    /** Issue or resend, and phrase the reply. */
    public AuthReply issue(HttpServletRequest request, String rawEmail, String ip, boolean resend) {
        IssueResult result = resend ? otp.resendCode(rawEmail, ip) : otp.requestCode(rawEmail, ip);
        String addr = Emails.normalize(rawEmail).orElse(rawEmail);
        return switch (result.outcome()) {
            case SENT -> {
                session.setPendingEmail(request, addr);
                String note = result.liveDelivery() ? ""
                        : " (Dev mode: email delivery is simulated — configure SMTP to receive the code.)";
                yield AuthReply.of("Perfect! I've sent a 6-digit verification code to " + addr
                        + ". Send me the code here so I can verify it's you." + note, AuthState.CODE_SENT, addr, false);
            }
            case INVALID_EMAIL -> AuthReply.of("That doesn't look like a valid email address. Mind trying again?",
                    AuthState.EMAIL_PENDING, null, false);
            case RATE_LIMITED -> AuthReply.of("Let's slow down for a moment — that's a lot of codes. "
                    + "Try again in a little while.", AuthState.CODE_SENT, addr, false);
            case DELIVERY_FAILED -> AuthReply.of("I couldn't send that email just now. Please try again in a moment.",
                    AuthState.EMAIL_PENDING, addr, false);
        };
    }

    /** Verify a submitted code and, on success, establish the session. */
    public AuthReply verify(HttpServletRequest request, HttpServletResponse response, String email, String code) {
        VerifyResult result = otp.verifyCode(email, code);
        return switch (result.outcome()) {
            case SUCCESS -> {
                UserAccount account = accounts.recordVerifiedLogin(email);
                session.authenticate(request, response, account.getEmail(), account.getRole());
                yield AuthReply.of("You're verified! Welcome to Sarah AI Agent. Your account is ready — "
                        + "what would you like to work on today?", AuthState.AUTHENTICATED, account.getEmail(), true);
            }
            case INVALID -> AuthReply.of("That code didn't match. " + result.attemptsRemaining()
                    + " attempt" + (result.attemptsRemaining() == 1 ? "" : "s") + " left — give it another go.",
                    AuthState.CODE_SENT, email, false);
            case EXPIRED -> AuthReply.of("That code has expired. Say “resend” and I'll send a fresh one.",
                    AuthState.EXPIRED, email, false);
            case LOCKED -> AuthReply.of("Too many attempts, so I've locked that code for safety. "
                    + "Say “resend” for a new one.", AuthState.LOCKED, email, false);
            case NO_CHALLENGE -> AuthReply.of("I don't have a code on file for you yet. What's your email address?",
                    AuthState.EMAIL_PENDING, null, false);
        };
    }

    /** Current state for GET /session. */
    public AuthReply currentState(HttpServletRequest request) {
        if (session.isAuthenticated()) {
            return AuthReply.of("Signed in.", AuthState.AUTHENTICATED, session.currentEmail(), true);
        }
        String pending = session.pendingEmail(request);
        if (pending != null) {
            return AuthReply.of("Awaiting your verification code.", AuthState.CODE_SENT, pending, false);
        }
        return AuthReply.of("Guest.", AuthState.GUEST, null, false);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        session.logout(request, response);
    }
}
