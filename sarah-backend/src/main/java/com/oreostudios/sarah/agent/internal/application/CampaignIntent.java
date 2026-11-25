package com.oreostudios.sarah.agent.internal.application;

import java.util.Locale;
import java.util.regex.Pattern;

/** Conservative action allowlist: a marketing discussion is not a request to publish. */
final class CampaignIntent {
    private static final Pattern EXPLICIT = Pattern.compile(
            "^(?:please\\s+)?(?:launch|create|start|build|plan)\\s+(?:a\\s+|an\\s+|new\\s+|the\\s+)*(?:marketing\\s+)?campaign\\b.+",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FRENCH = Pattern.compile(
            "^(?:lance|crée|cree|démarre|demarre|prépare|prepare)\\s+(?:une\\s+|la\\s+)?campagne\\b.+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL);
    static boolean isExplicit(String message) {
        if(message==null)return false;
        String text=message.strip();
        return (text.toLowerCase(Locale.ROOT).startsWith("/campaign ") && text.substring(10).strip().length()>0)
                || EXPLICIT.matcher(text).matches() || FRENCH.matcher(text).matches();
    }
    private CampaignIntent(){}
}
