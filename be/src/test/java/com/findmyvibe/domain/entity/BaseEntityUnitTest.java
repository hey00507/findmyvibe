package com.findmyvibe.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityUnitTest {

    @Test
    @DisplayName("BaseEntity를 상속한 엔티티는 createdAt, modifiedAt이 자동 초기화된다")
    void auditFieldsInitialized() {
        Session session = Session.create();

        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("모든 엔티티가 BaseEntity를 상속한다")
    void allEntitiesExtendBaseEntity() {
        assertThat(BaseEntity.class).isAssignableFrom(Session.class);
        assertThat(BaseEntity.class).isAssignableFrom(Question.class);
        assertThat(BaseEntity.class).isAssignableFrom(Answer.class);
        assertThat(BaseEntity.class).isAssignableFrom(Profile.class);
        assertThat(BaseEntity.class).isAssignableFrom(Recommendation.class);
    }
}
