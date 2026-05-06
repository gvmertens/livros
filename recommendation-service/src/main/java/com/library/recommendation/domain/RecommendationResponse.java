package com.library.recommendation.domain;

import java.util.List;

/**
 * Contract for the recommendations response.
 * Requirements: 10.3
 */
public record RecommendationResponse(List<String> recommendations) {}
