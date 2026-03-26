package com.findmyvibe.infrastructure.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmyvibe.common.exception.ClaudeApiException;
import com.findmyvibe.domain.service.AiAnalysisPort.AnalysisResult;
import com.findmyvibe.domain.service.AiAnalysisPort.FollowUpGenerationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeResponseParserTest {

    private final ClaudeResponseParser parser = new ClaudeResponseParser(new ObjectMapper());

    @Nested
    @DisplayName("꼬리질문 파싱")
    class FollowUpParsing {

        @Test
        @DisplayName("유효한 JSON에서 꼬리질문 3~5개를 파싱한다")
        void parsesValidFollowUpQuestions() {
            String json = """
                    {"followUpQuestions":["질문1","질문2","질문3"]}""";

            FollowUpGenerationResult result = parser.parseFollowUpQuestions(json);

            assertThat(result.questions()).hasSize(3);
            assertThat(result.questions().get(0)).isEqualTo("질문1");
        }

        @Test
        @DisplayName("마크다운 펜스가 있어도 파싱한다")
        void parsesJsonWithMarkdownFences() {
            String json = """
                    ```json
                    {"followUpQuestions":["질문1","질문2","질문3"]}
                    ```""";

            FollowUpGenerationResult result = parser.parseFollowUpQuestions(json);

            assertThat(result.questions()).hasSize(3);
        }

        @Test
        @DisplayName("잘못된 JSON이면 ClaudeApiException을 던진다")
        void throwsOnInvalidJson() {
            assertThatThrownBy(() -> parser.parseFollowUpQuestions("not json"))
                    .isInstanceOf(ClaudeApiException.class)
                    .hasMessageContaining("파싱 실패");
        }

        @Test
        @DisplayName("followUpQuestions 필드가 없으면 예외를 던진다")
        void throwsWhenFieldMissing() {
            assertThatThrownBy(() -> parser.parseFollowUpQuestions("{\"other\":\"value\"}"))
                    .isInstanceOf(ClaudeApiException.class)
                    .hasMessageContaining("followUpQuestions");
        }

        @Test
        @DisplayName("질문이 2개 이하면 예외를 던진다")
        void throwsWhenTooFewQuestions() {
            String json = """
                    {"followUpQuestions":["질문1","질문2"]}""";

            assertThatThrownBy(() -> parser.parseFollowUpQuestions(json))
                    .isInstanceOf(ClaudeApiException.class)
                    .hasMessageContaining("3~5개");
        }
    }

    @Nested
    @DisplayName("분석 결과 파싱")
    class AnalysisParsing {

        private static final String VALID_ANALYSIS_JSON = """
                {
                  "profile": {
                    "typeLabel": "감성적 탐험가",
                    "description": "자연 속에서 새로운 경험을 즐기는 성향",
                    "keywords": ["자연", "탐험", "감성"],
                    "traits": {
                      "creativity": 85,
                      "sociability": 60,
                      "activity": 70,
                      "exploration": 90,
                      "focus": 45
                    }
                  },
                  "recommendations": [
                    {
                      "hobbyName": "도시 스케칭",
                      "category": "예술",
                      "matchScore": 92,
                      "reason": "산책을 좋아하고 감성적인 성향에 잘 맞습니다"
                    }
                  ]
                }""";

        @Test
        @DisplayName("유효한 JSON에서 프로필과 추천을 파싱한다")
        void parsesValidAnalysisResult() {
            AnalysisResult result = parser.parseAnalysisResult(VALID_ANALYSIS_JSON);

            assertThat(result.profile().typeLabel()).isEqualTo("감성적 탐험가");
            assertThat(result.profile().keywords()).containsExactly("자연", "탐험", "감성");
            assertThat(result.profile().traits()).containsKeys("creativity", "sociability", "activity", "exploration", "focus");
            assertThat(result.profile().traits().get("creativity")).isEqualTo(85);
            assertThat(result.recommendations()).hasSize(1);
            assertThat(result.recommendations().get(0).hobbyName()).isEqualTo("도시 스케칭");
        }

        @Test
        @DisplayName("마크다운 펜스가 있어도 파싱한다")
        void parsesWithMarkdownFences() {
            String json = "```json\n" + VALID_ANALYSIS_JSON + "\n```";

            AnalysisResult result = parser.parseAnalysisResult(json);

            assertThat(result.profile().typeLabel()).isEqualTo("감성적 탐험가");
        }

        @Test
        @DisplayName("profile 필드가 없으면 예외를 던진다")
        void throwsWhenProfileMissing() {
            String json = """
                    {"recommendations":[]}""";

            assertThatThrownBy(() -> parser.parseAnalysisResult(json))
                    .isInstanceOf(ClaudeApiException.class)
                    .hasMessageContaining("profile");
        }

        @Test
        @DisplayName("traits에 필수 키가 없으면 예외를 던진다")
        void throwsWhenTraitsKeyMissing() {
            String json = """
                    {
                      "profile": {
                        "typeLabel": "테스트",
                        "description": "설명",
                        "keywords": ["키워드"],
                        "traits": {"creativity": 50}
                      },
                      "recommendations": []
                    }""";

            assertThatThrownBy(() -> parser.parseAnalysisResult(json))
                    .isInstanceOf(ClaudeApiException.class)
                    .hasMessageContaining("필수 키");
        }

        @Test
        @DisplayName("잘못된 JSON이면 ClaudeApiException을 던진다")
        void throwsOnInvalidJson() {
            assertThatThrownBy(() -> parser.parseAnalysisResult("{broken"))
                    .isInstanceOf(ClaudeApiException.class);
        }
    }

    @Nested
    @DisplayName("마크다운 펜스 제거")
    class StripMarkdownFences {

        @Test
        @DisplayName("```json 펜스를 제거한다")
        void stripsJsonFence() {
            String input = "```json\n{\"key\":\"value\"}\n```";
            assertThat(parser.stripMarkdownFences(input)).isEqualTo("{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("펜스가 없으면 그대로 반환한다")
        void returnsAsIsWithoutFence() {
            String input = "{\"key\":\"value\"}";
            assertThat(parser.stripMarkdownFences(input)).isEqualTo(input);
        }
    }
}
