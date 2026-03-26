package com.findmyvibe.infrastructure.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeMessageRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<Message> messages
) {
    public record Message(String role, String content) {
    }

    public static ClaudeMessageRequest of(String model, int maxTokens, String system, String userMessage) {
        return new ClaudeMessageRequest(model, maxTokens, system,
                List.of(new Message("user", userMessage)));
    }
}
