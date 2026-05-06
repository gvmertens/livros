package com.library.recommendation.api;

import com.library.recommendation.application.RecommendationService;
import com.library.recommendation.domain.RecommendationResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoint for book recommendations.
 *
 * <p>Returns personalised recommendations for the authenticated user based on
 * their accumulated rating and review history. Returns an empty list when no
 * history is available or when the LLM call fails.
 *
 * Requirements: 10.4
 */
@Path("/api/v1/recommendations")
@RolesAllowed({"USER", "ADMIN"})
@Produces(MediaType.APPLICATION_JSON)
public class RecommendationResource {

    @Inject
    RecommendationService recommendationService;

    @Inject
    JsonWebToken jwt;

    @GET
    public RecommendationResponse getRecommendations(@Context SecurityContext securityContext) {
        String sub = jwt.getSubject();
        if (sub == null) {
            return new RecommendationResponse(List.of());
        }

        try {
            UUID userId = UUID.fromString(sub);
            List<String> recommendations = recommendationService.recommend(userId);
            return new RecommendationResponse(recommendations);
        } catch (IllegalArgumentException e) {
            return new RecommendationResponse(List.of());
        }
    }
}
