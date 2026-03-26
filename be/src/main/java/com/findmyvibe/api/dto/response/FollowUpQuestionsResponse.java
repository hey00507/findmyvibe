package com.findmyvibe.api.dto.response;

import java.util.List;

public record FollowUpQuestionsResponse(
        List<QuestionResponse> followUpQuestions
) {
}
