package com.findmyvibe.domain.repository;

import com.findmyvibe.common.config.JpaAuditingConfig;
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
class QuestionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    @DisplayName("세션의 질문을 orderIndex 순서로 조회한다")
    void findBySessionOrderByOrderIndex() {
        Session session = Session.create();
        em.persist(session);

        em.persist(Question.createBasic(session, "질문3", 3));
        em.persist(Question.createBasic(session, "질문1", 1));
        em.persist(Question.createBasic(session, "질문2", 2));
        em.flush();
        em.clear();

        List<Question> questions = questionRepository.findBySessionOrderByOrderIndex(session);

        assertThat(questions).hasSize(3);
        assertThat(questions.get(0).getContent()).isEqualTo("질문1");
        assertThat(questions.get(1).getContent()).isEqualTo("질문2");
        assertThat(questions.get(2).getContent()).isEqualTo("질문3");
    }

    @Test
    @DisplayName("다른 세션의 질문은 조회되지 않는다")
    void findBySessionExcludesOtherSessions() {
        Session session1 = Session.create();
        Session session2 = Session.create();
        em.persist(session1);
        em.persist(session2);

        em.persist(Question.createBasic(session1, "세션1 질문", 1));
        em.persist(Question.createBasic(session2, "세션2 질문", 1));
        em.flush();
        em.clear();

        List<Question> questions = questionRepository.findBySessionOrderByOrderIndex(session1);

        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).getContent()).isEqualTo("세션1 질문");
    }
}
