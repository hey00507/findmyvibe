package com.findmyvibe.domain.entity;

import com.findmyvibe.domain.enums.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionUnitTest {

    @Nested
    @DisplayName("Session 생성")
    class Creation {

        @Test
        @DisplayName("새 세션은 CREATED 상태로 생성된다")
        void newSessionHasCreatedStatus() {
            Session session = Session.create();

            assertThat(session.getId()).isNotNull();
            assertThat(session.getStatus()).isEqualTo(SessionStatus.CREATED);
            assertThat(session.getCreatedAt()).isNotNull();
            assertThat(session.getCompletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Session 상태 전이")
    class StatusTransition {

        @Test
        @DisplayName("CREATED → BASIC_ANSWERED 전이 성공")
        void transitionToBasicAnswered() {
            Session session = Session.create();

            session.markBasicAnswered();

            assertThat(session.getStatus()).isEqualTo(SessionStatus.BASIC_ANSWERED);
        }

        @Test
        @DisplayName("BASIC_ANSWERED → COMPLETED 전이 성공")
        void transitionToCompleted() {
            Session session = Session.create();
            session.markBasicAnswered();

            session.markCompleted();

            assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
            assertThat(session.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("CREATED → COMPLETED 직접 전이 실패")
        void cannotSkipToCompleted() {
            Session session = Session.create();

            assertThatThrownBy(session::markCompleted)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPLETED → BASIC_ANSWERED 역행 전이 실패")
        void cannotGoBackFromCompleted() {
            Session session = Session.create();
            session.markBasicAnswered();
            session.markCompleted();

            assertThatThrownBy(session::markBasicAnswered)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("BASIC_ANSWERED → BASIC_ANSWERED 중복 전이 실패")
        void cannotTransitionToSameStatus() {
            Session session = Session.create();
            session.markBasicAnswered();

            assertThatThrownBy(session::markBasicAnswered)
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
