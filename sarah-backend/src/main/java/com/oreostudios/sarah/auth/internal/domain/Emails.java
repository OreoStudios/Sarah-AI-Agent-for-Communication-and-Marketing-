package com.oreostudios.sarah.auth.internal.domain; // Domain layer of the auth module.

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Email parsing/validation helpers. */
public final class Emails {

    // Pragmatic, conservative email pattern (not full RFC 5322, deliberately).
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    private Emails() { }

    /** @return the normalised (trimmed, lower-cased) email if valid */
    public static Optional<String> normalize(String raw) {
        if (raw == null) return Optional.empty();
        String t = raw.strip();
        return EMAIL.matcher(t).matches() ? Optional.of(t.toLowerCase(Locale.ROOT)) : Optional.empty();
    }

    /** @return the first email found anywhere in free text (for natural-language messages) */
    public static Optional<String> extract(String text) {
        if (text == null) return Optional.empty();
        var m = EMAIL.matcher(text);
        return m.find() ? Optional.of(m.group().toLowerCase(Locale.ROOT)) : Optional.empty();
    }
}
