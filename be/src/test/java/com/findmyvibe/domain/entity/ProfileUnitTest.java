package com.findmyvibe.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileUnitTest {

    @Test
    @DisplayName("프로필을 생성할 수 있다")
    void createProfile() {
        Session session = Session.create();
        List<String> keywords = List.of("창의적", "야외활동", "사교적");
        Map<String, Integer> traits = Map.of(
                "creativity", 85,
                "sociability", 60,
                "activity", 70,
                "exploration", 90,
                "focus", 45
        );

        Profile profile = Profile.create(session, "감성적 탐험가",
                "자연 속에서 새로운 경험을 즐기며 감성적인 활동을 선호합니다.",
                keywords, traits);

        assertThat(profile.getSession()).isEqualTo(session);
        assertThat(profile.getTypeLabel()).isEqualTo("감성적 탐험가");
        assertThat(profile.getDescription()).contains("자연");
        assertThat(profile.getKeywords()).hasSize(3);
        assertThat(profile.getTraits()).containsEntry("creativity", 85);
    }
}
