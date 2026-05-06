package com.library.identity.api;

import com.library.identity.api.dto.LoginRequest;
import com.library.identity.api.dto.LoginResponse;
import com.library.identity.api.dto.RegisterRequest;
import com.library.identity.api.dto.UserResponse;
import com.library.identity.application.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/register")
    public Response register(@Valid RegisterRequest request) {
        var appRequest = new com.library.identity.application.dto.RegisterRequest(
                request.name(), request.email(), request.password());
        var appResponse = authService.register(appRequest);
        var apiResponse = new UserResponse(
                appResponse.id(), appResponse.name(), appResponse.email(),
                appResponse.role(), appResponse.createdAt());
        return Response.status(201).entity(apiResponse).build();
    }

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest request) {
        var appRequest = new com.library.identity.application.dto.LoginRequest(
                request.email(), request.password());
        var appResponse = authService.login(appRequest);
        var apiResponse = new LoginResponse(appResponse.token(), appResponse.expiresIn());
        return Response.ok(apiResponse).build();
    }
}
