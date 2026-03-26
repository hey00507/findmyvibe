package com.findmyvibe.domain.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BasicQuestionsTest {

    @Test
    @DisplayName("기본 질문은 7개이다")
    void hasSevenQuestions() {
        assertThat(BasicQuestions.CONTENTS).hasSize(7);
        assertThat(BasicQuestions.COUNT).isEqualTo(7);
    }

    @Test
    @DisplayName("기본 질문 목록은 불변이다")
    void questionsAreImmutable() {
        assertThat(BasicQuestions.CONTENTS).isUnmodifiable();
    }

    @Test
    @DisplayName("모든 질문은 빈 문자열이 아니다")
    void allQuestionsAreNotBlank() {
        BasicQuestions.CONTENTS.forEach(q ->
                assertThat(q).isNotBlank()
        );
    }
}
