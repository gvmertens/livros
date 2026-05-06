package com.library.identity.api;

import com.library.identity.api.dto.UpdateRoleRequest;
import com.library.identity.api.dto.UserResponse;
import com.library.identity.application.UserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @GET
    @Path("/me")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getMe(@Context SecurityContext sec) {
        UUID currentUserId = UUID.fromString(sec.getUserPrincipal().getName());
        var appResponse = userService.getCurrentUser(currentUserId);
        var apiResponse = new UserResponse(
                appResponse.id(), appResponse.name(), appResponse.email(),
                appResponse.role(), appResponse.createdAt());
        return Response.ok(apiResponse).build();
    }

    @PUT
    @Path("/{id}/role")
    @RolesAllowed("ADMIN")
    public Response updateRole(@PathParam("id") UUID targetId, @Valid UpdateRoleRequest request) {
        userService.updateRole(targetId, request.role());
        var appResponse = userService.getCurrentUser(targetId);
        var apiResponse = new UserResponse(
                appResponse.id(), appResponse.name(), appResponse.email(),
                appResponse.role(), appResponse.createdAt());
        return Response.ok(apiResponse).build();
    }
}
