package com.findmyvibe.domain.service;

import java.util.List;
import java.util.Map;

public interface AiAnalysisPort {

    FollowUpGenerationResult generateFollowUpQuestions(List<QaPair> basicQaPairs);

    AnalysisResult analyzeAndRecommend(List<QaPair> allQaPairs);

    record QaPair(String question, String answer) {
    }

    record FollowUpGenerationResult(List<String> questions) {
    }

    record AnalysisResult(ProfileData profile, List<RecommendationData> recommendations) {
    }

    record ProfileData(
            String typeLabel,
            String description,
            List<String> keywords,
            Map<String, Integer> traits
    ) {
    }

    record RecommendationData(
            String hobbyName,
            String category,
            int matchScore,
            String reason
    ) {
    }
}
