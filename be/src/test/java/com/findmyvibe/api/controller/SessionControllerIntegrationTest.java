package com.findmyvibe.api.controller;

import com.findmyvibe.domain.constant.BasicQuestions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/sessions → 200, sessionId + 질문 7개 반환")
    void createSession_returnsSessionIdAndQuestions() throws Exception {
        mockMvc.perform(post("/api/v1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.questions", hasSize(BasicQuestions.COUNT)))
                .andExpect(jsonPath("$.questions[0].id").isNumber())
                .andExpect(jsonPath("$.questions[0].content").isNotEmpty())
                .andExpect(jsonPath("$.questions[0].orderIndex").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/sessions → 질문 순서가 1~7이다")
    void createSession_questionsOrderedCorrectly() throws Exception {
        mockMvc.perform(post("/api/v1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].orderIndex").value(1))
                .andExpect(jsonPath("$.questions[6].orderIndex").value(7));
    }

    @Test
    @DisplayName("POST /api/v1/sessions → 매번 다른 sessionId가 생성된다")
    void createSession_generatesUniqueSessionIds() throws Exception {
        String firstSessionId = mockMvc.perform(post("/api/v1/sessions"))
                .andReturn().getResponse().getContentAsString();

        String secondSessionId = mockMvc.perform(post("/api/v1/sessions"))
                .andReturn().getResponse().getContentAsString();

        // 응답 전체가 다르면 sessionId도 다름 (UUID가 포함되므로)
        org.assertj.core.api.Assertions.assertThat(firstSessionId)
                .isNotEqualTo(secondSessionId);
    }

    @Test
    @DisplayName("POST /api/v1/sessions → 첫 번째 질문 내용이 PRD와 일치한다")
    void createSession_firstQuestionMatchesPrd() throws Exception {
        mockMvc.perform(post("/api/v1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].content")
                        .value(BasicQuestions.CONTENTS.get(0)));
    }
}
