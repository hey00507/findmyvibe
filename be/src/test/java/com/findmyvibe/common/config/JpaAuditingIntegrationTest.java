package com.findmyvibe.common.config;

import com.findmyvibe.domain.entity.Session;
import com.findmyvibe.domain.enums.SessionStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("local")
class JpaAuditingIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("엔티티 저장 시 createdAt, createdBy가 자동으로 채워진다")
    void auditFieldsPopulatedOnPersist() {
        Session session = Session.create();

        entityManager.persist(session);
        entityManager.flush();
        entityManager.clear();

        Session found = entityManager.find(Session.class, session.getId());

        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getCreatedBy()).isEqualTo("system");
        assertThat(found.getModifiedAt()).isNotNull();
        assertThat(found.getModifiedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("엔티티 수정 시 modifiedAt, modifiedBy가 갱신된다")
    void auditFieldsUpdatedOnModify() {
        Session session = Session.create();
        entityManager.persist(session);
        entityManager.flush();
        entityManager.clear();

        Session found = entityManager.find(Session.class, session.getId());
        var originalModifiedAt = found.getModifiedAt();

        found.markBasicAnswered();
        entityManager.flush();
        entityManager.clear();

        Session updated = entityManager.find(Session.class, session.getId());

        assertThat(updated.getStatus()).isEqualTo(SessionStatus.BASIC_ANSWERED);
        assertThat(updated.getModifiedAt()).isAfterOrEqualTo(originalModifiedAt);
        assertThat(updated.getModifiedBy()).isEqualTo("system");
    }
}
