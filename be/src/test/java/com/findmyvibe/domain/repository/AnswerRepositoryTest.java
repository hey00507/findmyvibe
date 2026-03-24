package com.findmyvibe.domain.repository;

import com.findmyvibe.common.config.JpaAuditingConfig;
import com.findmyvibe.domain.entity.Answer;
import com.findmyvibe.domain.entity.Question;
import com.findmyvibe.domain.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("local")
class AnswerRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AnswerRepository answerRepository;

    @Test
    @DisplayName("세션 ID로 답변 목록을 조회한다")
    void findBySessionId() {
        Session session = Session.create();
        em.persist(session);
        Question q1 = Question.createBasic(session, "질문1", 1);
        Question q2 = Question.createBasic(session, "질문2", 2);
        em.persist(q1);
        em.persist(q2);
        em.persist(Answer.create(session, q1, "답변1"));
        em.persist(Answer.create(session, q2, "답변2"));
        em.flush();
        em.clear();

        List<Answer> answers = answerRepository.findBySessionId(session.getId());

        assertThat(answers).hasSize(2);
    }

    @Test
    @DisplayName("다른 세션의 답변은 조회되지 않는다")
    void findBySessionIdExcludesOtherSessions() {
        Session session1 = Session.create();
        Session session2 = Session.create();
        em.persist(session1);
        em.persist(session2);
        Question q1 = Question.createBasic(session1, "질문", 1);
        Question q2 = Question.createBasic(session2, "질문", 1);
        em.persist(q1);
        em.persist(q2);
        em.persist(Answer.create(session1, q1, "답변1"));
        em.persist(Answer.create(session2, q2, "답변2"));
        em.flush();
        em.clear();

        List<Answer> answers = answerRepository.findBySessionId(session1.getId());

        assertThat(answers).hasSize(1);
    }
}
