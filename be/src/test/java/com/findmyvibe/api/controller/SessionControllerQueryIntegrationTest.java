package com.findmyvibe.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmyvibe.api.dto.request.SubmitAnswersRequest;
import com.findmyvibe.api.dto.request.SubmitAnswersRequest.AnswerItem;
import com.findmyvibe.domain.service.AiAnalysisPort;
import com.findmyvibe.domain.service.AiAnalysisPort.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SessionControllerQueryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private AiAnalysisPort aiAnalysisPort;

    private String completedSessionId;

    @BeforeEach
    void setUp() throws Exception {
        when(aiAnalysisPort.generateFollowUpQuestions(anyList()))
                .thenReturn(new FollowUpGenerationResult(List.of("꼬리1", "꼬리2", "꼬리3")));

        ProfileData profile = new ProfileData("감성적 탐험가", "자연을 좋아하는 성향",
                List.of("자연", "감성"),
                Map.of("creativity", 85, "sociability", 60, "activity", 70, "exploration", 90, "focus", 45));
        RecommendationData rec1 = new RecommendationData("도시 스케칭", "예술", 92, "잘 맞습니다");
        RecommendationData rec2 = new RecommendationData("트레킹", "운동", 88, "야외 활동 추천");
        when(aiAnalysisPort.analyzeAndRecommend(anyList()))
                .thenReturn(new AnalysisResult(profile, List.of(rec1, rec2)));

        completedSessionId = completeFullFlow();
    }

    @Test
    @DisplayName("GET /profile → 완료된 세션의 프로필 조회")
    void getProfile_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}/profile", completedSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeLabel").value("감성적 탐험가"))
                .andExpect(jsonPath("$.description").value("자연을 좋아하는 성향"))
                .andExpect(jsonPath("$.keywords", hasSize(2)))
                .andExpect(jsonPath("$.traits.creativity").value(85));
    }

    @Test
    @DisplayName("GET /recommendations → 완료된 세션의 추천 목록 조회")
    void getRecommendations_returnsRecommendations() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}/recommendations", completedSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].hobbyName").value("도시 스케칭"))
                .andExpect(jsonPath("$[0].orderIndex").value(1))
                .andExpect(jsonPath("$[1].hobbyName").value("트레킹"));
    }

    @Test
    @DisplayName("GET /profile → 존재하지 않는 세션은 404")
    void getProfile_returns404ForUnknownSession() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/{id}/profile", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /profile → 미완료 세션은 400")
    void getProfile_returns400ForIncompleteSession() throws Exception {
        // 새 세션 생성 (CREATED 상태)
        MvcResult result = mockMvc.perform(post("/api/v1/sessions"))
                .andReturn();
        String sessionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("sessionId").asText();

        mockMvc.perform(get("/api/v1/sessions/{id}/profile", sessionId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /recommendations → 미완료 세션은 400")
    void getRecommendations_returns400ForIncompleteSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions"))
                .andReturn();
        String sessionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("sessionId").asText();

        mockMvc.perform(get("/api/v1/sessions/{id}/recommendations", sessionId))
                .andExpect(status().isBadRequest());
    }

    private String completeFullFlow() throws Exception {
        // 1. 세션 생성
        MvcResult createResult = mockMvc.perform(post("/api/v1/sessions"))
                .andReturn();
        JsonNode createBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String sessionId = createBody.get("sessionId").asText();

        // 2. 기본 답변 제출
        List<AnswerItem> basicAnswers = new ArrayList<>();
        for (JsonNode q : createBody.get("questions")) {
            basicAnswers.add(new AnswerItem(q.get("id").asLong(), "답변입니다"));
        }
        MvcResult answersResult = mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(basicAnswers))))
                .andReturn();

        // 3. 꼬리질문 답변 제출
        JsonNode followUps = objectMapper.readTree(answersResult.getResponse().getContentAsString())
                .get("followUpQuestions");
        List<AnswerItem> followUpAnswers = new ArrayList<>();
        for (JsonNode q : followUps) {
            followUpAnswers.add(new AnswerItem(q.get("id").asLong(), "꼬리답변입니다"));
        }
        mockMvc.perform(post("/api/v1/sessions/{id}/follow-up", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(followUpAnswers))))
                .andExpect(status().isOk());

        return sessionId;
    }
}
