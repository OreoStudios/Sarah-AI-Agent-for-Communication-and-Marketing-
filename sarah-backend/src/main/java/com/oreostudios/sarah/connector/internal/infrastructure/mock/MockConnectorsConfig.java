package com.oreostudios.sarah.connector.internal.infrastructure.mock; // Mock adapters (infrastructure).

import com.oreostudios.sarah.shared.Platform; // Platform enum.
import org.springframework.context.annotation.Bean; // Bean declarations.
import org.springframework.context.annotation.Configuration; // Configuration class.

/**
 * Registers mock connectors for platforms that need real OAuth/app approval. Each carries the
 * concrete setup requirement so the platform reports accurately.
 */
@Configuration // Registers mock connector beans.
public class MockConnectorsConfig {

    @Bean
    public MockConnector linkedInConnector() {
        return new MockConnector(Platform.LINKEDIN,
                "LinkedIn Marketing/Share API access: OAuth2 app + authorized Page + w_member_social / w_organization_social scopes");
    }

    @Bean
    public MockConnector facebookConnector() {
        return new MockConnector(Platform.FACEBOOK,
                "Meta Graph API: Facebook App review, Page access token, pages_manage_posts permission");
    }

    @Bean
    public MockConnector instagramConnector() {
        return new MockConnector(Platform.INSTAGRAM,
                "Meta Graph API: Instagram professional account linked to a Facebook Page + instagram_content_publish permission");
    }

    @Bean
    public MockConnector redditConnector() {
        return new MockConnector(Platform.REDDIT,
                "Reddit API OAuth2 script/web app, respecting each subreddit's rules and rate limits");
    }

    @Bean
    public MockConnector websiteConnector() {
        return new MockConnector(Platform.WEBSITE,
                "Configured CMS publishing integration (e.g. headless CMS API token)");
    }
}
