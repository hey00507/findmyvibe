package com.findmyvibe.domain.service;

import com.findmyvibe.common.exception.InvalidSessionStateException;
import com.findmyvibe.common.exception.SessionNotFoundException;
import com.findmyvibe.domain.entity.*;
import com.findmyvibe.domain.enums.QuestionType;
import com.findmyvibe.domain.enums.SessionStatus;
import com.findmyvibe.domain.repository.*;
import com.findmyvibe.domain.service.AiAnalysisPort.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private AnswerRepository answerRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private RecommendationRepository recommendationRepository;
    @Mock private AiAnalysisPort aiAnalysisPort;

    @InjectMocks
    private AnalysisService analysisService;

    @Nested
    @DisplayName("기본 답변 제출 (submitBasicAnswers)")
    class SubmitBasicAnswers {

        @Test
        @DisplayName("정상 제출 시 꼬리질문을 반환한다")
        void returnsFollowUpQuestions() {
            Session session = Session.create();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            Question q1 = Question.createBasic(session, "질문1", 1);
            setId(q1, 1L);
            when(questionRepository.findBySessionOrderByOrderIndex(session)).thenReturn(List.of(q1));
            when(answerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(questionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(aiAnalysisPort.generateFollowUpQuestions(anyList()))
                    .thenReturn(new FollowUpGenerationResult(List.of("꼬리1", "꼬리2", "꼬리3")));

            List<Question> result = analysisService.submitBasicAnswers(
                    session.getId(), Map.of(1L, "답변1"));

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getType()).isEqualTo(QuestionType.FOLLOW_UP);
            assertThat(session.getStatus()).isEqualTo(SessionStatus.BASIC_ANSWERED);
        }

        @Test
        @DisplayName("존재하지 않는 세션이면 SessionNotFoundException")
        void throwsWhenSessionNotFound() {
            UUID id = UUID.randomUUID();
            when(sessionRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analysisService.submitBasicAnswers(id, Map.of(1L, "답변")))
                    .isInstanceOf(SessionNotFoundException.class);
        }

        @Test
        @DisplayName("세션 상태가 CREATED가 아니면 InvalidSessionStateException")
        void throwsWhenInvalidState() {
            Session session = Session.create();
            session.markBasicAnswered();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> analysisService.submitBasicAnswers(
                    session.getId(), Map.of(1L, "답변")))
                    .isInstanceOf(InvalidSessionStateException.class);
        }

        @Test
        @DisplayName("답변 수가 질문 수와 다르면 IllegalArgumentException")
        void throwsWhenAnswerCountMismatch() {
            Session session = Session.create();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            Question q1 = Question.createBasic(session, "질문1", 1);
            setId(q1, 1L);
            Question q2 = Question.createBasic(session, "질문2", 2);
            setId(q2, 2L);
            when(questionRepository.findBySessionOrderByOrderIndex(session)).thenReturn(List.of(q1, q2));

            assertThatThrownBy(() -> analysisService.submitBasicAnswers(
                    session.getId(), Map.of(1L, "답변1")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("답변 개수");
        }
    }

    @Nested
    @DisplayName("꼬리질문 답변 제출 (submitFollowUpAnswers)")
    class SubmitFollowUpAnswers {

        @Test
        @DisplayName("정상 제출 시 분석 결과를 반환한다")
        void returnsAnalysisResult() {
            Session session = Session.create();
            session.markBasicAnswered();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            Question basic = Question.createBasic(session, "기본질문", 1);
            setId(basic, 1L);
            Question followUp = Question.createFollowUp(session, "꼬리질문", 1);
            setId(followUp, 2L);
            when(questionRepository.findBySessionOrderByOrderIndex(session))
                    .thenReturn(List.of(basic, followUp));

            when(answerRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            Answer basicAnswer = Answer.create(session, basic, "기본답변");
            Answer followUpAnswer = Answer.create(session, followUp, "꼬리답변");
            when(answerRepository.findBySessionId(session.getId()))
                    .thenReturn(List.of(basicAnswer, followUpAnswer));

            ProfileData profileData = new ProfileData("탐험가", "설명", List.of("키워드"),
                    Map.of("creativity", 80, "sociability", 60, "activity", 70, "exploration", 90, "focus", 50));
            RecommendationData rec = new RecommendationData("스케칭", "예술", 90, "이유");
            when(aiAnalysisPort.analyzeAndRecommend(anyList()))
                    .thenReturn(new AnalysisResult(profileData, List.of(rec)));
            when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            AnalysisResult result = analysisService.submitFollowUpAnswers(
                    session.getId(), Map.of(2L, "꼬리답변"));

            assertThat(result.profile().typeLabel()).isEqualTo("탐험가");
            assertThat(result.recommendations()).hasSize(1);
            assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        }

        @Test
        @DisplayName("세션 상태가 BASIC_ANSWERED가 아니면 InvalidSessionStateException")
        void throwsWhenInvalidState() {
            Session session = Session.create();
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> analysisService.submitFollowUpAnswers(
                    session.getId(), Map.of(1L, "답변")))
                    .isInstanceOf(InvalidSessionStateException.class);
        }
    }

    private void setId(Question question, Long id) {
        try {
            var field = Question.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(question, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
