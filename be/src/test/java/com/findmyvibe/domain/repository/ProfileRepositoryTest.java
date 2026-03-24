package com.findmyvibe.domain.repository;

import com.findmyvibe.common.config.JpaAuditingConfig;
import com.findmyvibe.domain.entity.Profile;
import com.findmyvibe.domain.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("local")
class ProfileRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    @DisplayName("세션 ID로 프로필을 조회한다")
    void findBySessionId() {
        Session session = Session.create();
        em.persist(session);
        em.persist(Profile.create(session, "감성적 탐험가", "설명",
                List.of("창의적"), Map.of("creativity", 85)));
        em.flush();
        em.clear();

        Optional<Profile> profile = profileRepository.findBySessionId(session.getId());

        assertThat(profile).isPresent();
        assertThat(profile.get().getTypeLabel()).isEqualTo("감성적 탐험가");
    }

    @Test
    @DisplayName("존재하지 않는 세션 ID로 조회하면 빈 Optional을 반환한다")
    void findBySessionIdReturnsEmptyForNonExistent() {
        Session session = Session.create();
        em.persist(session);
        em.flush();
        em.clear();

        Optional<Profile> profile = profileRepository.findBySessionId(session.getId());

        assertThat(profile).isEmpty();
    }
}
