package com.findmyvibe.api.dto.response;

import java.util.List;
import java.util.UUID;

public record CreateSessionResponse(
        UUID sessionId,
        List<QuestionResponse> questions
) {
}
