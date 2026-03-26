package com.findmyvibe.infrastructure.claude;

import com.findmyvibe.common.exception.ClaudeApiException;
import com.findmyvibe.domain.service.AiAnalysisPort;
import com.findmyvibe.infrastructure.claude.dto.ClaudeMessageRequest;
import com.findmyvibe.infrastructure.claude.dto.ClaudeMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeClient implements AiAnalysisPort {

    private final RestClient claudeRestClient;
    private final ClaudeProperties properties;
    private final ClaudePromptBuilder promptBuilder;
    private final ClaudeResponseParser responseParser;

    @Override
    public FollowUpGenerationResult generateFollowUpQuestions(List<QaPair> basicQaPairs) {
        ClaudePromptBuilder.PromptPair prompt = promptBuilder.buildFollowUpPrompt(basicQaPairs);
        String responseText = callClaudeApi(prompt);
        return responseParser.parseFollowUpQuestions(responseText);
    }

    @Override
    public AnalysisResult analyzeAndRecommend(List<QaPair> allQaPairs) {
        ClaudePromptBuilder.PromptPair prompt = promptBuilder.buildAnalysisPrompt(allQaPairs);
        String responseText = callClaudeApi(prompt);
        return responseParser.parseAnalysisResult(responseText);
    }

    private String callClaudeApi(ClaudePromptBuilder.PromptPair prompt) {
        ClaudeMessageRequest request = ClaudeMessageRequest.of(
                properties.model(), properties.maxTokens(),
                prompt.system(), prompt.user());

        try {
            ClaudeMessageResponse response = claudeRestClient.post()
                    .uri("/v1/messages")
                    .body(request)
                    .retrieve()
                    .body(ClaudeMessageResponse.class);

            if (response == null) {
                throw new ClaudeApiException("Claude API 응답이 null입니다");
            }

            log.info("Claude API 호출 완료 - 입력: {}토큰, 출력: {}토큰",
                    response.usage().inputTokens(), response.usage().outputTokens());

            return response.extractText();
        } catch (RestClientException e) {
            throw new ClaudeApiException("Claude API 호출 실패: " + e.getMessage(), e);
        }
    }
}
