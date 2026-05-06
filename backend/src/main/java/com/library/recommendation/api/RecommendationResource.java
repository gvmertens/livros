package com.library.recommendation.api;

import com.library.recommendation.domain.RecommendationResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/recommendations")
@RolesAllowed({"USER", "ADMIN"})
@Produces(MediaType.APPLICATION_JSON)
public class RecommendationResource {

    @GET
    public RecommendationResponse getRecommendations() {
        return new RecommendationResponse(List.of());
    }
}
