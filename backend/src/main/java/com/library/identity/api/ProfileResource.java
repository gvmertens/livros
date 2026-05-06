package com.library.identity.api;

import com.library.identity.api.dto.ProfileResponse;
import com.library.identity.api.dto.ProfileUpdateRequest;
import com.library.identity.application.ProfileService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/api/v1/users/me/profile")
@RolesAllowed({"USER", "ADMIN"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @Inject
    ProfileService profileService;

    @GET
    public Response getProfile(@Context SecurityContext sec) {
        UUID currentUserId = UUID.fromString(sec.getUserPrincipal().getName());
        var appResponse = profileService.getProfile(currentUserId, currentUserId);
        var apiResponse = toApiProfileResponse(appResponse);
        return Response.ok(apiResponse).build();
    }

    @PUT
    public Response updateProfile(@Context SecurityContext sec, @Valid ProfileUpdateRequest request) {
        UUID currentUserId = UUID.fromString(sec.getUserPrincipal().getName());
        var appRequest = new com.library.identity.application.dto.ProfileUpdateRequest(
                request.displayName(), request.bio(), request.favoriteGenres());
        var appResponse = profileService.updateProfile(currentUserId, appRequest);
        var apiResponse = toApiProfileResponse(appResponse);
        return Response.ok(apiResponse).build();
    }

    private ProfileResponse toApiProfileResponse(com.library.identity.application.dto.ProfileResponse appResponse) {
        return new ProfileResponse(
                appResponse.id(),
                appResponse.userId(),
                appResponse.displayName(),
                appResponse.bio(),
                appResponse.favoriteGenres(),
                appResponse.createdAt(),
                appResponse.updatedAt());
    }
}
