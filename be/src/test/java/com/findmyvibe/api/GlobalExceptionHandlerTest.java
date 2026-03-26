package com.findmyvibe.api;

import com.findmyvibe.common.exception.InvalidSessionStateException;
import com.findmyvibe.common.exception.SessionNotFoundException;
import com.findmyvibe.domain.enums.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("SessionNotFoundException → 404 ProblemDetail")
    void handleSessionNotFound_returns404() {
        UUID id = UUID.randomUUID();
        SessionNotFoundException ex = new SessionNotFoundException(id);

        ProblemDetail result = handler.handleSessionNotFound(ex);

        assertThat(result.getStatus()).isEqualTo(404);
        assertThat(result.getTitle()).isEqualTo("Not Found");
        assertThat(result.getDetail()).contains(id.toString());
    }

    @Test
    @DisplayName("InvalidSessionStateException → 400 ProblemDetail")
    void handleInvalidSessionState_returns400() {
        InvalidSessionStateException ex = new InvalidSessionStateException(
                SessionStatus.COMPLETED, SessionStatus.BASIC_ANSWERED);

        ProblemDetail result = handler.handleInvalidSessionState(ex);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getTitle()).isEqualTo("Bad Request");
        assertThat(result.getDetail()).contains("COMPLETED").contains("BASIC_ANSWERED");
    }
}
