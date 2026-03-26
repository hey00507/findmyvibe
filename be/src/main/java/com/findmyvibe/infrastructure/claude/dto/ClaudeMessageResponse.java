package com.findmyvibe.infrastructure.claude.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeMessageResponse(
        String id,
        String type,
        List<Content> content,
        Usage usage
) {
    public record Content(String type, String text) {
    }

    public record Usage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) {
    }

    public String extractText() {
        return content.stream()
                .filter(c -> "text".equals(c.type()))
                .map(Content::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claude 응답에서 텍스트를 찾을 수 없습니다"));
    }
}
