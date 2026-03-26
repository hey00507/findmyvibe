package com.findmyvibe.api.dto.response;

import com.findmyvibe.domain.entity.Profile;

import java.util.List;
import java.util.Map;

public record ProfileResponse(
        String typeLabel,
        String description,
        List<String> keywords,
        Map<String, Integer> traits
) {
    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.getTypeLabel(),
                profile.getDescription(),
                profile.getKeywords(),
                profile.getTraits()
        );
    }
}
