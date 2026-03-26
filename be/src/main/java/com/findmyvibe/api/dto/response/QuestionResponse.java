package com.findmyvibe.api.dto.response;

import com.findmyvibe.domain.entity.Question;

public record QuestionResponse(
        Long id,
        String content,
        int orderIndex
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getContent(),
                question.getOrderIndex()
        );
    }
}
