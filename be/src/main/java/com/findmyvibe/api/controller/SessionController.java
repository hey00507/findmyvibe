package com.findmyvibe.api.controller;

import com.findmyvibe.api.dto.response.CreateSessionResponse;
import com.findmyvibe.api.dto.response.QuestionResponse;
import com.findmyvibe.domain.service.SessionService;
import com.findmyvibe.domain.service.SessionService.SessionCreateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;



import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public CreateSessionResponse createSession() {
        SessionCreateResult result = sessionService.createSession();

        List<QuestionResponse> questionResponses = result.questions().stream()
                .map(QuestionResponse::from)
                .toList();

        return new CreateSessionResponse(result.session().getId(), questionResponses);
    }
}
