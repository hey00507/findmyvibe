package com.findmyvibe.api.dto.response;

import java.util.List;

public record AnalysisResponse(
        ProfileResponse profile,
        List<RecommendationResponse> recommendations
) {
}
