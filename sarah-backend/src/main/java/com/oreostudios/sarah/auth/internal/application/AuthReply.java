package com.oreostudios.sarah.auth.internal.application; // Application layer of the auth module.

import com.oreostudios.sarah.auth.internal.domain.AuthState;

/**
 * Sarah's response to an onboarding/auth message.
 *
 * @param reply          what Sarah says (safe for display; never contains the code)
 * @param state          the authentication state
 * @param email          the email in play, if any
 * @param authenticated  whether the session is now authenticated
 */
public record AuthReply(String reply, AuthState state, String email, boolean authenticated) {

    static AuthReply of(String reply, AuthState state, String email, boolean authenticated) {
        return new AuthReply(reply, state, email, authenticated);
    }
}
