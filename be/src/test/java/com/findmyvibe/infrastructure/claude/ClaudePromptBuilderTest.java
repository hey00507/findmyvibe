package com.findmyvibe.infrastructure.claude;

import com.findmyvibe.domain.service.AiAnalysisPort.QaPair;
import com.findmyvibe.infrastructure.claude.ClaudePromptBuilder.PromptPair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClaudePromptBuilderTest {

    private final ClaudePromptBuilder builder = new ClaudePromptBuilder();

    private final List<QaPair> basicPairs = List.of(
            new QaPair("혼자 시간을 보낼 때 주로 뭘 하나요?", "넷플릭스 봐요"),
            new QaPair("새로운 걸 배울 때 어떤 방식을 선호하나요?", "직접 해보기"),
            new QaPair("실내 vs 야외?", "야외")
    );

    @Test
    @DisplayName("꼬리질문 프롬프트에 모든 Q&A가 포함된다")
    void followUpPrompt_containsAllQaPairs() {
        PromptPair result = builder.buildFollowUpPrompt(basicPairs);

        assertThat(result.user()).contains("Q1:", "A1:", "Q2:", "A2:", "Q3:", "A3:");
        assertThat(result.user()).contains("넷플릭스 봐요", "직접 해보기", "야외");
    }

    @Test
    @DisplayName("꼬리질문 시스템 프롬프트에 JSON 형식 지시가 포함된다")
    void followUpPrompt_systemContainsJsonInstruction() {
        PromptPair result = builder.buildFollowUpPrompt(basicPairs);

        assertThat(result.system()).contains("followUpQuestions");
        assertThat(result.system()).contains("JSON");
    }

    @Test
    @DisplayName("분석 프롬프트에 모든 Q&A가 포함된다")
    void analysisPrompt_containsAllQaPairs() {
        PromptPair result = builder.buildAnalysisPrompt(basicPairs);

        assertThat(result.user()).contains("Q1:", "A1:", "Q3:", "A3:");
    }

    @Test
    @DisplayName("분석 시스템 프롬프트에 5축 traits와 추천 형식이 포함된다")
    void analysisPrompt_systemContainsTraitsAndRecommendations() {
        PromptPair result = builder.buildAnalysisPrompt(basicPairs);

        assertThat(result.system())
                .contains("creativity", "sociability", "activity", "exploration", "focus")
                .contains("hobbyName", "matchScore")
                .contains("typeLabel", "keywords");
    }

    @Test
    @DisplayName("프롬프트는 한국어로 작성된다")
    void prompts_areInKorean() {
        PromptPair followUp = builder.buildFollowUpPrompt(basicPairs);
        PromptPair analysis = builder.buildAnalysisPrompt(basicPairs);

        assertThat(followUp.system()).contains("당신은");
        assertThat(analysis.system()).contains("당신은");
    }
}
