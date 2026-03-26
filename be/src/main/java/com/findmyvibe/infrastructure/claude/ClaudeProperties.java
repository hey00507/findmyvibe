package com.findmyvibe.infrastructure.claude;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "claude")
public record ClaudeProperties(
        String apiKey,
        String model,
        String baseUrl,
        int maxTokens
) {
}
