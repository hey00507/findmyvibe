package com.findmyvibe.api.controller;

import com.findmyvibe.api.dto.request.SubmitAnswersRequest;
import com.findmyvibe.api.dto.response.*;
import com.findmyvibe.domain.entity.Question;
import com.findmyvibe.domain.service.AiAnalysisPort.AnalysisResult;
import com.findmyvibe.domain.service.AnalysisService;
import com.findmyvibe.domain.service.SessionService;
import com.findmyvibe.domain.service.SessionService.SessionCreateResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Session", description = "AI 성향 분석 세션 API")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final AnalysisService analysisService;

    @Operation(summary = "세션 생성", description = "새 세션을 생성하고 기본 질문 7개를 반환합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public CreateSessionResponse createSession() {
        SessionCreateResult result = sessionService.createSession();

        List<QuestionResponse> questionResponses = result.questions().stream()
                .map(QuestionResponse::from)
                .toList();

        return new CreateSessionResponse(result.session().getId(), questionResponses);
    }

    @Operation(summary = "기본 답변 제출", description = "기본 질문 7개에 대한 답변을 제출하면 꼬리질문 3~5개를 반환합니다")
    @PostMapping("/{sessionId}/answers")
    public FollowUpQuestionsResponse submitAnswers(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswersRequest request) {

        Map<Long, String> answerMap = toAnswerMap(request.answers());
        List<Question> followUpQuestions = analysisService.submitBasicAnswers(sessionId, answerMap);

        List<QuestionResponse> responses = followUpQuestions.stream()
                .map(QuestionResponse::from)
                .toList();

        return new FollowUpQuestionsResponse(responses);
    }

    @Operation(summary = "꼬리질문 답변 제출", description = "꼬리질문 답변을 제출하면 성향 프로필 + 추천 5개를 반환합니다")
    @PostMapping("/{sessionId}/follow-up")
    public AnalysisResponse submitFollowUp(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswersRequest request) {

        Map<Long, String> answerMap = toAnswerMap(request.answers());
        AnalysisResult result = analysisService.submitFollowUpAnswers(sessionId, answerMap);

        ProfileResponse profile = new ProfileResponse(
                result.profile().typeLabel(),
                result.profile().description(),
                result.profile().keywords(),
                result.profile().traits());

        List<RecommendationResponse> recommendations = new java.util.ArrayList<>();
        for (int i = 0; i < result.recommendations().size(); i++) {
            var rd = result.recommendations().get(i);
            recommendations.add(new RecommendationResponse(
                    rd.hobbyName(), rd.category(), rd.matchScore(), rd.reason(), i + 1));
        }

        return new AnalysisResponse(profile, recommendations);
    }

    @Operation(summary = "프로필 조회", description = "완료된 세션의 성향 프로필을 조회합니다 (캐시 적용)")
    @GetMapping("/{sessionId}/profile")
    public ProfileResponse getProfile(@PathVariable UUID sessionId) {
        return ProfileResponse.from(analysisService.getProfile(sessionId));
    }

    @Operation(summary = "추천 목록 조회", description = "완료된 세션의 추천 취미/클래스 목록을 조회합니다 (캐시 적용)")
    @GetMapping("/{sessionId}/recommendations")
    public List<RecommendationResponse> getRecommendations(@PathVariable UUID sessionId) {
        return analysisService.getRecommendations(sessionId).stream()
                .map(RecommendationResponse::from)
                .toList();
    }

    private Map<Long, String> toAnswerMap(List<SubmitAnswersRequest.AnswerItem> items) {
        Map<Long, String> map = new LinkedHashMap<>();
        items.forEach(item -> map.put(item.questionId(), item.content()));
        return map;
    }
}
