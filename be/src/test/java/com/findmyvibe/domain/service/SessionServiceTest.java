package com.findmyvibe.domain.service;

import com.findmyvibe.domain.constant.BasicQuestions;
import com.findmyvibe.domain.entity.Question;
import com.findmyvibe.domain.entity.Session;
import com.findmyvibe.domain.enums.QuestionType;
import com.findmyvibe.domain.enums.SessionStatus;
import com.findmyvibe.domain.repository.QuestionRepository;
import com.findmyvibe.domain.repository.SessionRepository;
import com.findmyvibe.domain.service.SessionService.SessionCreateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private SessionService sessionService;

    @Captor
    private ArgumentCaptor<Session> sessionCaptor;

    @Captor
    private ArgumentCaptor<List<Question>> questionsCaptor;

    @Test
    @DisplayName("세션 생성 시 CREATED 상태의 세션이 저장된다")
    void createSession_savesSessionWithCreatedStatus() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SessionCreateResult result = sessionService.createSession();

        verify(sessionRepository).save(sessionCaptor.capture());
        Session saved = sessionCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.CREATED);
        assertThat(saved.getId()).isNotNull();
        assertThat(result.session()).isSameAs(saved);
    }

    @Test
    @DisplayName("세션 생성 시 기본 질문 7개가 저장된다")
    void createSession_saveSevenBasicQuestions() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SessionCreateResult result = sessionService.createSession();

        verify(questionRepository).saveAll(questionsCaptor.capture());
        assertThat(questionsCaptor.getValue()).hasSize(BasicQuestions.COUNT);
        assertThat(result.questions()).hasSize(BasicQuestions.COUNT);
    }

    @Test
    @DisplayName("저장되는 질문은 모두 BASIC 타입이다")
    void createSession_allQuestionsAreBasicType() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        sessionService.createSession();

        verify(questionRepository).saveAll(questionsCaptor.capture());
        questionsCaptor.getValue().forEach(q ->
                assertThat(q.getType()).isEqualTo(QuestionType.BASIC)
        );
    }

    @Test
    @DisplayName("저장되는 질문의 orderIndex는 1부터 시작한다")
    void createSession_orderIndexStartsFromOne() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        sessionService.createSession();

        verify(questionRepository).saveAll(questionsCaptor.capture());
        List<Question> questions = questionsCaptor.getValue();
        for (int i = 0; i < questions.size(); i++) {
            assertThat(questions.get(i).getOrderIndex()).isEqualTo(i + 1);
        }
    }

    @Test
    @DisplayName("createSession은 세션과 질문 목록을 함께 반환한다")
    void createSession_returnsBothSessionAndQuestions() {
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        SessionCreateResult result = sessionService.createSession();

        assertThat(result.session()).isNotNull();
        assertThat(result.questions()).isNotEmpty();
        assertThat(result.session().getId()).isNotNull();
    }
}
