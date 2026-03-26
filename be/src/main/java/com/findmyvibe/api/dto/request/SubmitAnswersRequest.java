package com.findmyvibe.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAnswersRequest(
        @Valid @NotNull List<AnswerItem> answers
) {
    public record AnswerItem(
            @NotNull Long questionId,
            @NotBlank String content
    ) {
    }
}
