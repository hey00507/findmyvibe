package com.findmyvibe.api.dto.response;

import com.findmyvibe.domain.entity.Recommendation;

public record RecommendationResponse(
        String hobbyName,
        String category,
        int matchScore,
        String reason,
        int orderIndex
) {
    public static RecommendationResponse from(Recommendation recommendation) {
        return new RecommendationResponse(
                recommendation.getHobbyName(),
                recommendation.getCategory(),
                recommendation.getMatchScore(),
                recommendation.getReason(),
                recommendation.getOrderIndex()
        );
    }
}
