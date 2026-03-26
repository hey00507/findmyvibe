package com.findmyvibe.domain.service;

import com.findmyvibe.common.exception.InvalidSessionStateException;
import com.findmyvibe.common.exception.SessionNotFoundException;
import com.findmyvibe.domain.entity.*;
import com.findmyvibe.domain.enums.QuestionType;
import com.findmyvibe.domain.enums.SessionStatus;
import com.findmyvibe.domain.repository.*;
import com.findmyvibe.domain.service.AiAnalysisPort.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ProfileRepository profileRepository;
    private final RecommendationRepository recommendationRepository;
    private final AiAnalysisPort aiAnalysisPort;

    @Transactional
    public List<Question> submitBasicAnswers(UUID sessionId, Map<Long, String> answersByQuestionId) {
        Session session = findSession(sessionId);
        validateStatus(session, SessionStatus.CREATED);

        List<Question> basicQuestions = questionRepository.findBySessionOrderByOrderIndex(session);
        validateAnswerCount(answersByQuestionId.size(), basicQuestions.size());

        Map<Long, Question> questionMap = basicQuestions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Answer> answers = new ArrayList<>();
        answersByQuestionId.forEach((questionId, content) -> {
            Question question = questionMap.get(questionId);
            if (question == null) {
                throw new IllegalArgumentException("존재하지 않는 질문 ID: " + questionId);
            }
            answers.add(Answer.create(session, question, content));
        });
        answerRepository.saveAll(answers);

        List<QaPair> qaPairs = basicQuestions.stream()
                .map(q -> new QaPair(q.getContent(), answersByQuestionId.get(q.getId())))
                .toList();

        FollowUpGenerationResult followUpResult = aiAnalysisPort.generateFollowUpQuestions(qaPairs);

        List<Question> followUpQuestions = new ArrayList<>();
        for (int i = 0; i < followUpResult.questions().size(); i++) {
            followUpQuestions.add(Question.createFollowUp(
                    session, followUpResult.questions().get(i), i + 1));
        }
        questionRepository.saveAll(followUpQuestions);

        session.markBasicAnswered();

        return followUpQuestions;
    }

    @Transactional
    public AnalysisResult submitFollowUpAnswers(UUID sessionId, Map<Long, String> answersByQuestionId) {
        Session session = findSession(sessionId);
        validateStatus(session, SessionStatus.BASIC_ANSWERED);

        List<Question> allQuestions = questionRepository.findBySessionOrderByOrderIndex(session);
        List<Question> followUpQuestions = allQuestions.stream()
                .filter(q -> q.getType() == QuestionType.FOLLOW_UP)
                .toList();

        validateAnswerCount(answersByQuestionId.size(), followUpQuestions.size());

        Map<Long, Question> questionMap = followUpQuestions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Answer> answers = new ArrayList<>();
        answersByQuestionId.forEach((questionId, content) -> {
            Question question = questionMap.get(questionId);
            if (question == null) {
                throw new IllegalArgumentException("존재하지 않는 꼬리질문 ID: " + questionId);
            }
            answers.add(Answer.create(session, question, content));
        });
        answerRepository.saveAll(answers);

        List<Answer> allAnswers = answerRepository.findBySessionId(sessionId);
        Map<Long, String> allAnswerMap = allAnswers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Answer::getContent));

        List<QaPair> allQaPairs = allQuestions.stream()
                .map(q -> new QaPair(q.getContent(), allAnswerMap.get(q.getId())))
                .toList();

        AnalysisResult analysisResult = aiAnalysisPort.analyzeAndRecommend(allQaPairs);

        ProfileData profileData = analysisResult.profile();
        Profile profile = Profile.create(session, profileData.typeLabel(), profileData.description(),
                profileData.keywords(), profileData.traits());
        profileRepository.save(profile);

        List<Recommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < analysisResult.recommendations().size(); i++) {
            RecommendationData rd = analysisResult.recommendations().get(i);
            recommendations.add(Recommendation.create(
                    session, rd.hobbyName(), rd.category(), rd.matchScore(), rd.reason(), i + 1));
        }
        recommendationRepository.saveAll(recommendations);

        session.markCompleted();

        return analysisResult;
    }

    @Cacheable(value = "profile", key = "#sessionId")
    public Profile getProfile(UUID sessionId) {
        Session session = findSession(sessionId);
        validateStatus(session, SessionStatus.COMPLETED);
        return profileRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    @Cacheable(value = "recommendations", key = "#sessionId")
    public List<Recommendation> getRecommendations(UUID sessionId) {
        Session session = findSession(sessionId);
        validateStatus(session, SessionStatus.COMPLETED);
        return recommendationRepository.findBySessionIdOrderByOrderIndex(sessionId);
    }

    private Session findSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }

    private void validateStatus(Session session, SessionStatus required) {
        if (session.getStatus() != required) {
            throw new InvalidSessionStateException(session.getStatus(), required);
        }
    }

    private void validateAnswerCount(int actual, int expected) {
        if (actual != expected) {
            throw new IllegalArgumentException(
                    "답변 개수가 질문 수와 일치하지 않습니다. 답변: " + actual + ", 질문: " + expected);
        }
    }
}
