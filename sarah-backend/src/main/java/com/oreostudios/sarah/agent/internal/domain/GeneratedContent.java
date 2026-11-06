package com.oreostudios.sarah.agent.internal.domain; // Domain layer of the agent module.

/**
 * Content produced by the Content Agent for one platform.
 *
 * @param title      optional title (platform-dependent)
 * @param body       the content body
 * @param imageBrief a creative/image brief
 */
public record GeneratedContent(String title, String body, String imageBrief) {
}
