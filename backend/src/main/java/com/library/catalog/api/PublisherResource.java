package com.library.catalog.api;

import com.library.catalog.api.dto.PublisherRequest;
import com.library.catalog.api.dto.PublisherResponse;
import com.library.catalog.application.PublisherService;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/publishers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublisherResource {

    @Inject
    PublisherService publisherService;

    @POST
    @RolesAllowed("ADMIN")
    public Response create(@Valid PublisherRequest request) {
        var appRequest = new com.library.catalog.application.dto.PublisherRequest(request.name());
        var appResponse = publisherService.create(appRequest);
        var apiResponse = toApiResponse(appResponse);
        return Response.status(201).entity(apiResponse).build();
    }

    @GET
    @RolesAllowed({"USER", "ADMIN"})
    public Response list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        PageResponse<com.library.catalog.application.dto.PublisherResponse> appPage = publisherService.list(pageRequest);
        PageResponse<PublisherResponse> apiPage = mapPage(appPage);
        return Response.ok(apiPage).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getById(@PathParam("id") UUID id) {
        var appResponse = publisherService.getById(id);
        return Response.ok(toApiResponse(appResponse)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response update(@PathParam("id") UUID id, @Valid PublisherRequest request) {
        var appRequest = new com.library.catalog.application.dto.PublisherRequest(request.name());
        var appResponse = publisherService.update(id, appRequest);
        return Response.ok(toApiResponse(appResponse)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") UUID id) {
        publisherService.delete(id);
        return Response.noContent().build();
    }

    private PublisherResponse toApiResponse(com.library.catalog.application.dto.PublisherResponse app) {
        return new PublisherResponse(app.id(), app.name(), app.createdAt(), app.updatedAt());
    }

    private PageResponse<PublisherResponse> mapPage(PageResponse<com.library.catalog.application.dto.PublisherResponse> appPage) {
        var content = appPage.content().stream().map(this::toApiResponse).toList();
        return new PageResponse<>(content, appPage.totalElements(), appPage.totalPages(), appPage.page(), appPage.size());
    }
}
