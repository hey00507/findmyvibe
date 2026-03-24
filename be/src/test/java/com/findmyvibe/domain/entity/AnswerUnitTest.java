package com.findmyvibe.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerUnitTest {

    @Test
    @DisplayName("답변을 생성할 수 있다")
    void createAnswer() {
        Session session = Session.create();
        Question question = Question.createBasic(session, "질문 내용", 1);

        Answer answer = Answer.create(session, question, "넷플릭스 보거나 산책해요");

        assertThat(answer.getSession()).isEqualTo(session);
        assertThat(answer.getQuestion()).isEqualTo(question);
        assertThat(answer.getContent()).isEqualTo("넷플릭스 보거나 산책해요");
        assertThat(answer.getCreatedAt()).isNotNull();
    }
}
