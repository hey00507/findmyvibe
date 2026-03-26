package com.findmyvibe.infrastructure.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmyvibe.common.exception.ClaudeApiException;
import com.findmyvibe.domain.service.AiAnalysisPort.AnalysisResult;
import com.findmyvibe.domain.service.AiAnalysisPort.FollowUpGenerationResult;
import com.findmyvibe.domain.service.AiAnalysisPort.QaPair;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class ClaudeClientTest {

    private ClaudeClient claudeClient;

    private final List<QaPair> basicPairs = List.of(
            new QaPair("혼자 시간을 보낼 때 주로 뭘 하나요?", "넷플릭스 봐요"),
            new QaPair("새로운 걸 배울 때 어떤 방식을 선호하나요?", "직접 해보기"),
            new QaPair("실내 vs 야외?", "야외")
    );

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        ClaudeProperties properties = new ClaudeProperties(
                "test-api-key", "claude-sonnet-4-20250514",
                wmInfo.getHttpBaseUrl(), 4096);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .requestFactory(factory)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        ClaudePromptBuilder promptBuilder = new ClaudePromptBuilder();
        ClaudeResponseParser responseParser = new ClaudeResponseParser(objectMapper);

        claudeClient = new ClaudeClient(restClient, properties, promptBuilder, responseParser);
    }

    @Nested
    @DisplayName("꼬리질문 생성")
    class GenerateFollowUp {

        @Test
        @DisplayName("정상 응답 시 꼬리질문을 반환한다")
        void returnsFollowUpQuestions() {
            stubFor(post("/v1/messages")
                    .willReturn(okJson(claudeResponse("""
                            {"followUpQuestions":["산책할 때 주로 어디를 걷나요?","넷플릭스에서 어떤 장르를 보나요?","야외에서 주로 어떤 활동을 하나요?"]}"""))));

            FollowUpGenerationResult result = claudeClient.generateFollowUpQuestions(basicPairs);

            assertThat(result.questions()).hasSize(3);
            assertThat(result.questions().get(0)).contains("산책");
            verify(postRequestedFor(urlEqualTo("/v1/messages"))
                    .withHeader("x-api-key", equalTo("test-api-key")));
        }
    }

    @Nested
    @DisplayName("분석 + 추천")
    class AnalyzeAndRecommend {

        @Test
        @DisplayName("정상 응답 시 프로필과 추천을 반환한다")
        void returnsAnalysisResult() {
            stubFor(post("/v1/messages")
                    .willReturn(okJson(claudeResponse("""
                            {"profile":{"typeLabel":"감성적 탐험가","description":"자연을 좋아하는 성향","keywords":["자연","감성"],"traits":{"creativity":85,"sociability":60,"activity":70,"exploration":90,"focus":45}},"recommendations":[{"hobbyName":"도시 스케칭","category":"예술","matchScore":92,"reason":"잘 맞습니다"}]}"""))));

            AnalysisResult result = claudeClient.analyzeAndRecommend(basicPairs);

            assertThat(result.profile().typeLabel()).isEqualTo("감성적 탐험가");
            assertThat(result.recommendations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("에러 처리")
    class ErrorHandling {

        @Test
        @DisplayName("500 에러 시 ClaudeApiException을 던진다")
        void throwsOn500() {
            stubFor(post("/v1/messages")
                    .willReturn(serverError()));

            assertThatThrownBy(() -> claudeClient.generateFollowUpQuestions(basicPairs))
                    .isInstanceOf(ClaudeApiException.class);
        }

        @Test
        @DisplayName("429 에러 시 ClaudeApiException을 던진다")
        void throwsOn429() {
            stubFor(post("/v1/messages")
                    .willReturn(status(429)));

            assertThatThrownBy(() -> claudeClient.generateFollowUpQuestions(basicPairs))
                    .isInstanceOf(ClaudeApiException.class);
        }

        @Test
        @DisplayName("타임아웃 시 ClaudeApiException을 던진다")
        void throwsOnTimeout() {
            stubFor(post("/v1/messages")
                    .willReturn(ok().withFixedDelay(10000)));

            assertThatThrownBy(() -> claudeClient.generateFollowUpQuestions(basicPairs))
                    .isInstanceOf(ClaudeApiException.class);
        }
    }

    private String claudeResponse(String text) {
        return """
                {
                  "id": "msg_test",
                  "type": "message",
                  "content": [{"type": "text", "text": %s}],
                  "usage": {"input_tokens": 100, "output_tokens": 200}
                }""".formatted(escapeJsonValue(text));
    }

    private String escapeJsonValue(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
