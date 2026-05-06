package com.library.recommendation.domain;

import java.util.UUID;

/**
 * Contract for requesting recommendations for a user.
 * Requirements: 10.3
 */
public record RecommendationRequest(UUID userId) {}
