package com.findmyvibe.api.controller;

import com.findmyvibe.api.dto.request.SubmitAnswersRequest;
import com.findmyvibe.api.dto.response.*;
import com.findmyvibe.domain.entity.Question;
import com.findmyvibe.domain.service.AiAnalysisPort.AnalysisResult;
import com.findmyvibe.domain.service.AnalysisService;
import com.findmyvibe.domain.service.SessionService;
import com.findmyvibe.domain.service.SessionService.SessionCreateResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final AnalysisService analysisService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public CreateSessionResponse createSession() {
        SessionCreateResult result = sessionService.createSession();

        List<QuestionResponse> questionResponses = result.questions().stream()
                .map(QuestionResponse::from)
                .toList();

        return new CreateSessionResponse(result.session().getId(), questionResponses);
    }

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

    @GetMapping("/{sessionId}/profile")
    public ProfileResponse getProfile(@PathVariable UUID sessionId) {
        return ProfileResponse.from(analysisService.getProfile(sessionId));
    }

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
