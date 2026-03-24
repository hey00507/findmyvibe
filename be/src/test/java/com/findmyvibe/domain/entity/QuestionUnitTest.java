package com.findmyvibe.domain.entity;

import com.findmyvibe.domain.enums.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionUnitTest {

    @Test
    @DisplayName("기본 질문을 생성할 수 있다")
    void createBasicQuestion() {
        Session session = Session.create();

        Question question = Question.createBasic(session, "혼자 시간을 보낼 때 주로 뭘 하나요?", 1);

        assertThat(question.getSession()).isEqualTo(session);
        assertThat(question.getType()).isEqualTo(QuestionType.BASIC);
        assertThat(question.getContent()).isEqualTo("혼자 시간을 보낼 때 주로 뭘 하나요?");
        assertThat(question.getOrderIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("꼬리질문을 생성할 수 있다")
    void createFollowUpQuestion() {
        Session session = Session.create();

        Question question = Question.createFollowUp(session, "산책할 때 주로 어디를 걷나요?", 1);

        assertThat(question.getType()).isEqualTo(QuestionType.FOLLOW_UP);
    }
}
