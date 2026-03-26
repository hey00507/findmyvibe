package com.findmyvibe.infrastructure.claude;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmyvibe.common.exception.ClaudeApiException;
import com.findmyvibe.domain.service.AiAnalysisPort.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ClaudeResponseParser {

    private static final Set<String> REQUIRED_TRAITS = Set.of(
            "creativity", "sociability", "activity", "exploration", "focus");

    private final ObjectMapper objectMapper;

    public ClaudeResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FollowUpGenerationResult parseFollowUpQuestions(String rawJson) {
        String json = stripMarkdownFences(rawJson);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode questionsNode = root.get("followUpQuestions");
            if (questionsNode == null || !questionsNode.isArray()) {
                throw new ClaudeApiException("followUpQuestions 필드가 없거나 배열이 아닙니다: " + json);
            }

            List<String> questions = new ArrayList<>();
            questionsNode.forEach(node -> questions.add(node.asText()));

            if (questions.size() < 3 || questions.size() > 5) {
                throw new ClaudeApiException("꼬리질문은 3~5개여야 합니다. 실제: " + questions.size());
            }

            return new FollowUpGenerationResult(questions);
        } catch (JsonProcessingException e) {
            throw new ClaudeApiException("Claude 응답 JSON 파싱 실패: " + json, e);
        }
    }

    public AnalysisResult parseAnalysisResult(String rawJson) {
        String json = stripMarkdownFences(rawJson);
        try {
            JsonNode root = objectMapper.readTree(json);

            ProfileData profile = parseProfile(root.get("profile"));
            List<RecommendationData> recommendations = parseRecommendations(root.get("recommendations"));

            return new AnalysisResult(profile, recommendations);
        } catch (JsonProcessingException e) {
            throw new ClaudeApiException("Claude 응답 JSON 파싱 실패: " + json, e);
        }
    }

    private ProfileData parseProfile(JsonNode node) {
        if (node == null) {
            throw new ClaudeApiException("profile 필드가 없습니다");
        }

        String typeLabel = node.get("typeLabel").asText();
        String description = node.get("description").asText();

        List<String> keywords = new ArrayList<>();
        node.get("keywords").forEach(k -> keywords.add(k.asText()));

        Map<String, Integer> traits = new LinkedHashMap<>();
        JsonNode traitsNode = node.get("traits");
        REQUIRED_TRAITS.forEach(key -> {
            if (traitsNode.get(key) == null) {
                throw new ClaudeApiException("traits에 필수 키가 없습니다: " + key);
            }
            traits.put(key, traitsNode.get(key).asInt());
        });

        return new ProfileData(typeLabel, description, keywords, traits);
    }

    private List<RecommendationData> parseRecommendations(JsonNode node) {
        if (node == null || !node.isArray()) {
            throw new ClaudeApiException("recommendations 필드가 없거나 배열이 아닙니다");
        }

        List<RecommendationData> list = new ArrayList<>();
        node.forEach(item -> list.add(new RecommendationData(
                item.get("hobbyName").asText(),
                item.get("category").asText(),
                item.get("matchScore").asInt(),
                item.get("reason").asText()
        )));

        return list;
    }

    String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            return trimmed.trim();
        }
        return trimmed;
    }
}
