package com.oreostudios.sarah.config; // Application configuration module.

import org.springframework.boot.context.properties.ConfigurationProperties; // Binds external config.
import org.springframework.boot.context.properties.NestedConfigurationProperty; // Nested property metadata.

import java.util.List; // List-typed properties.

/** Strongly-typed configuration for the Sarah platform (prefix {@code sarah}). */
@ConfigurationProperties(prefix = "sarah") // Bound from application.yml / environment.
public class SarahProperties {

    @NestedConfigurationProperty
    private Llm llm = new Llm(); // LLM provider settings.
    @NestedConfigurationProperty
    private Security security = new Security(); // Security / secrets settings.
    @NestedConfigurationProperty
    private Autonomy autonomy = new Autonomy(); // Bootstrap autonomy defaults.
    @NestedConfigurationProperty
    private Http http = new Http(); // Outbound HTTP (SSRF) settings.
    @NestedConfigurationProperty
    private Auth auth = new Auth(); // Conversational OTP authentication.

    public Llm getLlm() { return llm; }
    public Security getSecurity() { return security; }
    public Autonomy getAutonomy() { return autonomy; }
    public Http getHttp() { return http; }
    public Auth getAuth() { return auth; }

    /** LLM provider configuration. */
    public static class Llm {
        /** mock (offline default) | anthropic */
        private String provider = "mock"; // Active provider.
        private Anthropic anthropic = new Anthropic(); // Anthropic/Claude settings.
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public Anthropic getAnthropic() { return anthropic; }
        public void setAnthropic(Anthropic anthropic) { this.anthropic = anthropic; }

        /** Anthropic Claude settings. */
        public static class Anthropic {
            private String apiKey = ""; // ANTHROPIC_API_KEY.
            private String model = "claude-opus-4-8"; // Default model id.
            private int maxTokens = 2048; // Max output tokens.
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
            public int getMaxTokens() { return maxTokens; }
            public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        }
    }

    /** Security and secrets configuration. */
    public static class Security {
        private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin(); // Seeded owner account.
        private Cors cors = new Cors(); // CORS allow-list.
        /** Base64 32-byte key for AES-GCM token encryption. Override in production. */
        private String tokenEncryptionKey = ""; // Connector-token encryption key.
        public BootstrapAdmin getBootstrapAdmin() { return bootstrapAdmin; }
        public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) { this.bootstrapAdmin = bootstrapAdmin; }
        public Cors getCors() { return cors; }
        public void setCors(Cors cors) { this.cors = cors; }
        public String getTokenEncryptionKey() { return tokenEncryptionKey; }
        public void setTokenEncryptionKey(String tokenEncryptionKey) { this.tokenEncryptionKey = tokenEncryptionKey; }

        /** Seeded owner account used on first run. */
        public static class BootstrapAdmin {
            private String email = "owner@oreostudios.com"; // Owner email.
            private String password = "change-me-now"; // Owner initial password.
            public String getEmail() { return email; }
            public void setEmail(String email) { this.email = email; }
            public String getPassword() { return password; }
            public void setPassword(String password) { this.password = password; }
        }

        /** Allowed browser origins for CORS. */
        public static class Cors {
            private List<String> allowedOrigins = List.of("http://localhost:5173"); // Dev frontend origin.
            public List<String> getAllowedOrigins() { return allowedOrigins; }
            public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        }
    }

    /** Bootstrap defaults for autonomy; the live values are persisted in the DB. */
    public static class Autonomy {
        private boolean enabled = true; // Kill-switch default.
        private int dailyPostLimit = 10; // Daily publication cap default.
        private int perPlatformCooldownMinutes = 30; // Cooldown between posts per platform.
        private int maxLlmCallsPerRun = 25; // LLM-call budget per orchestration run.
        private int maxAgentHops = 12; // Loop-protection hop cap.
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getDailyPostLimit() { return dailyPostLimit; }
        public void setDailyPostLimit(int dailyPostLimit) { this.dailyPostLimit = dailyPostLimit; }
        public int getPerPlatformCooldownMinutes() { return perPlatformCooldownMinutes; }
        public void setPerPlatformCooldownMinutes(int v) { this.perPlatformCooldownMinutes = v; }
        public int getMaxLlmCallsPerRun() { return maxLlmCallsPerRun; }
        public void setMaxLlmCallsPerRun(int v) { this.maxLlmCallsPerRun = v; }
        public int getMaxAgentHops() { return maxAgentHops; }
        public void setMaxAgentHops(int maxAgentHops) { this.maxAgentHops = maxAgentHops; }
    }

    /** Conversational email-OTP authentication. */
    public static class Auth {
        /** Base64/hex secret used to HMAC verification codes. MUST be set in production. */
        private String hmacSecret = "";
        private int otpTtlSeconds = 300;              // Codes expire after 5 minutes.
        private int maxAttempts = 5;                  // Max verification attempts per challenge.
        private int resendCooldownSeconds = 60;       // Minimum seconds between resends.
        private int maxCodesPerEmailPerHour = 5;      // Per-email issue rate limit.
        private int maxCodesPerIpPerHour = 20;        // Per-IP issue rate limit.
        private Email email = new Email();
        public String getHmacSecret() { return hmacSecret; }
        public void setHmacSecret(String hmacSecret) { this.hmacSecret = hmacSecret; }
        public int getOtpTtlSeconds() { return otpTtlSeconds; }
        public void setOtpTtlSeconds(int v) { this.otpTtlSeconds = v; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int v) { this.maxAttempts = v; }
        public int getResendCooldownSeconds() { return resendCooldownSeconds; }
        public void setResendCooldownSeconds(int v) { this.resendCooldownSeconds = v; }
        public int getMaxCodesPerEmailPerHour() { return maxCodesPerEmailPerHour; }
        public void setMaxCodesPerEmailPerHour(int v) { this.maxCodesPerEmailPerHour = v; }
        public int getMaxCodesPerIpPerHour() { return maxCodesPerIpPerHour; }
        public void setMaxCodesPerIpPerHour(int v) { this.maxCodesPerIpPerHour = v; }
        public Email getEmail() { return email; }
        public void setEmail(Email email) { this.email = email; }

        /** Email delivery settings. */
        public static class Email {
            /** log (dev, no real send) | smtp */
            private String provider = "log";
            private String from = "Sarah <sarah@oreostudios.com>";
            public String getProvider() { return provider; }
            public void setProvider(String provider) { this.provider = provider; }
            public String getFrom() { return from; }
            public void setFrom(String from) { this.from = from; }
        }
    }

    /** Outbound HTTP (SSRF) configuration. */
    public static class Http {
        /** SSRF allow-list: outbound tool fetches may only target these hosts. */
        private List<String> outboundAllowedHosts = List.of("discord.com", "discordapp.com"); // Allowed hosts.
        public List<String> getOutboundAllowedHosts() { return outboundAllowedHosts; }
        public void setOutboundAllowedHosts(List<String> hosts) { this.outboundAllowedHosts = hosts; }
    }
}
