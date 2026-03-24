package com.findmyvibe.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecommendationUnitTest {

    @Test
    @DisplayName("추천을 생성할 수 있다")
    void createRecommendation() {
        Session session = Session.create();

        Recommendation recommendation = Recommendation.create(
                session, "도시 스케칭", "예술", 92,
                "산책을 좋아하고 감성적인 성향에 잘 맞습니다", 1);

        assertThat(recommendation.getSession()).isEqualTo(session);
        assertThat(recommendation.getHobbyName()).isEqualTo("도시 스케칭");
        assertThat(recommendation.getCategory()).isEqualTo("예술");
        assertThat(recommendation.getMatchScore()).isEqualTo(92);
        assertThat(recommendation.getReason()).contains("산책");
        assertThat(recommendation.getClassUrl()).isNull();
        assertThat(recommendation.getOrderIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("matchScore는 0~100 사이여야 한다")
    void matchScoreMustBeInRange() {
        Session session = Session.create();

        assertThatThrownBy(() -> Recommendation.create(
                session, "취미", "카테고리", 101, "이유", 1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Recommendation.create(
                session, "취미", "카테고리", -1, "이유", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
