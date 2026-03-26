package com.findmyvibe.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findmyvibe.api.dto.request.SubmitAnswersRequest;
import com.findmyvibe.api.dto.request.SubmitAnswersRequest.AnswerItem;
import com.findmyvibe.domain.service.AiAnalysisPort;
import com.findmyvibe.domain.service.AiAnalysisPort.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SessionControllerAnswersIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private AiAnalysisPort aiAnalysisPort;

    @Test
    @DisplayName("POST /answers → 꼬리질문 반환")
    void submitAnswers_returnsFollowUpQuestions() throws Exception {
        when(aiAnalysisPort.generateFollowUpQuestions(anyList()))
                .thenReturn(new FollowUpGenerationResult(List.of("꼬리1", "꼬리2", "꼬리3")));

        SessionInfo session = createSessionAndGetInfo();
        List<AnswerItem> answers = buildBasicAnswers(session.questions());

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", session.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(answers))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followUpQuestions", hasSize(3)))
                .andExpect(jsonPath("$.followUpQuestions[0].content").value("꼬리1"));
    }

    @Test
    @DisplayName("POST /follow-up → 프로필 + 추천 반환")
    void submitFollowUp_returnsAnalysisResult() throws Exception {
        when(aiAnalysisPort.generateFollowUpQuestions(anyList()))
                .thenReturn(new FollowUpGenerationResult(List.of("꼬리1", "꼬리2", "꼬리3")));

        ProfileData profile = new ProfileData("감성적 탐험가", "자연을 좋아하는 성향",
                List.of("자연", "감성"),
                Map.of("creativity", 85, "sociability", 60, "activity", 70, "exploration", 90, "focus", 45));
        RecommendationData rec = new RecommendationData("도시 스케칭", "예술", 92, "잘 맞습니다");
        when(aiAnalysisPort.analyzeAndRecommend(anyList()))
                .thenReturn(new AnalysisResult(profile, List.of(rec)));

        SessionInfo session = createSessionAndGetInfo();

        // Step 1: 기본 답변 제출
        List<AnswerItem> basicAnswers = buildBasicAnswers(session.questions());
        MvcResult answersResult = mockMvc.perform(post("/api/v1/sessions/{id}/answers", session.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(basicAnswers))))
                .andExpect(status().isOk())
                .andReturn();

        // Step 2: 꼬리질문 답변 제출
        List<AnswerItem> followUpAnswers = extractAnswersFromResponse(answersResult, "followUpQuestions");

        mockMvc.perform(post("/api/v1/sessions/{id}/follow-up", session.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(followUpAnswers))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.typeLabel").value("감성적 탐험가"))
                .andExpect(jsonPath("$.profile.traits.creativity").value(85))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].hobbyName").value("도시 스케칭"));
    }

    @Test
    @DisplayName("존재하지 않는 세션에 답변 제출 시 404")
    void submitAnswers_returns404ForUnknownSession() throws Exception {
        UUID unknownId = UUID.randomUUID();
        List<AnswerItem> answers = List.of(new AnswerItem(1L, "답변"));

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(answers))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이미 답변 제출한 세션에 다시 제출 시 400")
    void submitAnswers_returns400ForInvalidState() throws Exception {
        when(aiAnalysisPort.generateFollowUpQuestions(anyList()))
                .thenReturn(new FollowUpGenerationResult(List.of("꼬리1", "꼬리2", "꼬리3")));

        SessionInfo session = createSessionAndGetInfo();
        List<AnswerItem> answers = buildBasicAnswers(session.questions());

        // 첫 번째 제출 (성공)
        mockMvc.perform(post("/api/v1/sessions/{id}/answers", session.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(answers))))
                .andExpect(status().isOk());

        // 두 번째 제출 (상태 오류)
        mockMvc.perform(post("/api/v1/sessions/{id}/answers", session.sessionId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(answers))))
                .andExpect(status().isBadRequest());
    }

    private record SessionInfo(String sessionId, JsonNode questions) {
    }

    private SessionInfo createSessionAndGetInfo() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return new SessionInfo(root.get("sessionId").asText(), root.get("questions"));
    }

    private List<AnswerItem> buildBasicAnswers(JsonNode questions) {
        List<AnswerItem> answers = new ArrayList<>();
        for (JsonNode q : questions) {
            answers.add(new AnswerItem(q.get("id").asLong(), "답변입니다"));
        }
        return answers;
    }

    private List<AnswerItem> extractAnswersFromResponse(MvcResult result, String arrayField) throws Exception {
        JsonNode nodes = objectMapper.readTree(result.getResponse().getContentAsString())
                .get(arrayField);
        List<AnswerItem> answers = new ArrayList<>();
        for (JsonNode node : nodes) {
            answers.add(new AnswerItem(node.get("id").asLong(), "꼬리답변입니다"));
        }
        return answers;
    }
}
