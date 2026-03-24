package com.findmyvibe.domain.repository;

import com.findmyvibe.common.config.JpaAuditingConfig;
import com.findmyvibe.domain.entity.Recommendation;
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
class RecommendationRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Test
    @DisplayName("세션 ID로 추천 목록을 orderIndex 순서로 조회한다")
    void findBySessionIdOrderByOrderIndex() {
        Session session = Session.create();
        em.persist(session);
        em.persist(Recommendation.create(session, "도시 스케칭", "예술", 92, "이유1", 3));
        em.persist(Recommendation.create(session, "러닝", "운동", 85, "이유2", 1));
        em.persist(Recommendation.create(session, "요리", "생활", 78, "이유3", 2));
        em.flush();
        em.clear();

        List<Recommendation> recommendations =
                recommendationRepository.findBySessionIdOrderByOrderIndex(session.getId());

        assertThat(recommendations).hasSize(3);
        assertThat(recommendations.get(0).getHobbyName()).isEqualTo("러닝");
        assertThat(recommendations.get(1).getHobbyName()).isEqualTo("요리");
        assertThat(recommendations.get(2).getHobbyName()).isEqualTo("도시 스케칭");
    }

    @Test
    @DisplayName("해당 세션의 추천이 없으면 빈 리스트를 반환한다")
    void findBySessionIdReturnsEmptyList() {
        Session session = Session.create();
        em.persist(session);
        em.flush();
        em.clear();

        List<Recommendation> recommendations =
                recommendationRepository.findBySessionIdOrderByOrderIndex(session.getId());

        assertThat(recommendations).isEmpty();
    }
}
