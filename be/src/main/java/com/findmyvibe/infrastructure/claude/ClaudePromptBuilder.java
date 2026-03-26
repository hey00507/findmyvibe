package com.findmyvibe.infrastructure.claude;

import com.findmyvibe.domain.service.AiAnalysisPort.QaPair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
public class ClaudePromptBuilder {

    static final String FOLLOW_UP_SYSTEM = """
            당신은 사용자의 성향을 분석하는 전문가입니다.
            사용자가 7개의 기본 질문에 답한 내용을 바탕으로, 성향을 더 정밀하게 파악하기 위한 꼬리질문 3~5개를 생성하세요.

            규칙:
            - 답변에서 모호하거나 더 탐색할 만한 부분을 파고드는 질문을 만드세요
            - 한국어로 자연스럽게 작성하세요
            - 반드시 아래 JSON 형식으로만 응답하세요 (다른 텍스트 없이)

            출력 형식:
            {"followUpQuestions":["질문1","질문2","질문3"]}""";

    static final String ANALYSIS_SYSTEM = """
            당신은 사용자의 성향을 분석하고 맞춤 취미/원데이클래스를 추천하는 전문가입니다.
            사용자의 모든 질문-답변을 종합하여 성향 프로필과 추천 5개를 생성하세요.

            규칙:
            - 성향 타입 라벨은 창의적이고 기억에 남는 한국어 표현으로 (예: "감성적 탐험가")
            - 설명은 2~3문장으로 작성
            - 키워드는 3~5개
            - traits 값은 0~100 사이 정수 (5축: creativity, sociability, activity, exploration, focus)
            - 추천은 정확히 5개, 구체적이고 실현 가능한 취미/클래스
            - matchScore는 0~100 사이 정수
            - 반드시 아래 JSON 형식으로만 응답하세요 (다른 텍스트 없이)

            출력 형식:
            {"profile":{"typeLabel":"...","description":"...","keywords":["..."],"traits":{"creativity":0,"sociability":0,"activity":0,"exploration":0,"focus":0}},"recommendations":[{"hobbyName":"...","category":"...","matchScore":0,"reason":"..."}]}""";

    public PromptPair buildFollowUpPrompt(List<QaPair> basicQaPairs) {
        String userMessage = formatQaPairs(basicQaPairs);
        return new PromptPair(FOLLOW_UP_SYSTEM, userMessage);
    }

    public PromptPair buildAnalysisPrompt(List<QaPair> allQaPairs) {
        String userMessage = formatQaPairs(allQaPairs);
        return new PromptPair(ANALYSIS_SYSTEM, userMessage);
    }

    private String formatQaPairs(List<QaPair> qaPairs) {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, qaPairs.size()).forEach(i -> {
            QaPair pair = qaPairs.get(i);
            sb.append("Q").append(i + 1).append(": ").append(pair.question()).append("\n");
            sb.append("A").append(i + 1).append(": ").append(pair.answer()).append("\n\n");
        });
        return sb.toString().trim();
    }

    public record PromptPair(String system, String user) {
    }
}
