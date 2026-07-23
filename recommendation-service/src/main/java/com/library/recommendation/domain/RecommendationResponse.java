package com.library.recommendation.domain;

import java.util.List;

/**
 * Contract for the recommendations response.
 * Requirements: 10.3
 */
public record RecommendationResponse(String criteriaSummary, List<BookRecommendation> recommendations) {
    public record BookRecommendation(String title, String publisher) {}
}
