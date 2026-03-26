package com.findmyvibe.api.dto.response;

import com.findmyvibe.domain.entity.Question;
import com.findmyvibe.domain.entity.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionResponseTest {

    @Test
    @DisplayName("Question 엔티티에서 QuestionResponse로 변환된다")
    void fromQuestion_mapsCorrectly() {
        Session session = Session.create();
        Question question = Question.createBasic(session, "테스트 질문", 3);

        QuestionResponse response = QuestionResponse.from(question);

        assertThat(response.content()).isEqualTo("테스트 질문");
        assertThat(response.orderIndex()).isEqualTo(3);
    }
}
