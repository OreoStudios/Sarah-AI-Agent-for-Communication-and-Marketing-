package com.oreostudios.sarah.config; // Application configuration module.

import io.swagger.v3.oas.models.OpenAPI; // OpenAPI document model.
import io.swagger.v3.oas.models.info.Info; // API info section.
import io.swagger.v3.oas.models.info.License; // API license section.
import org.springframework.boot.context.properties.EnableConfigurationProperties; // Activates @ConfigurationProperties beans.
import org.springframework.context.annotation.Bean; // Declares beans.
import org.springframework.context.annotation.Configuration; // Marks a configuration class.
import org.springframework.web.client.RestClient; // Synchronous HTTP client.

/** Core application beans: config properties, OpenAPI metadata, base {@link RestClient} builder. */
@Configuration // Spring configuration.
@EnableConfigurationProperties(SarahProperties.class) // Bind SarahProperties.
public class AppConfig {

    /** @return OpenAPI metadata shown in Swagger UI. */
    @Bean
    public OpenAPI sarahOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Sarah AI Agent API")
                .description("Autonomous multi-agent marketing platform for Oreo Studios")
                .version("0.1.0")
                .license(new License().name("Proprietary")));
    }

    /**
     * Base {@link RestClient.Builder}. All outbound HTTP from connectors passes through the
     * connector module's SSRF guard before any request is made.
     *
     * @return a fresh RestClient builder
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
