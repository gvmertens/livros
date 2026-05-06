package com.library.reading.api;

import com.library.reading.api.dto.BookSummary;
import com.library.reading.api.dto.CreateReadingRequest;
import com.library.reading.api.dto.ReadingResponse;
import com.library.reading.api.dto.UpdateReadingRequest;
import com.library.reading.application.ReadingService;
import com.library.reading.domain.ReadingStatus;
import com.library.shared.exception.ValidationException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/api/v1/readings")
@RolesAllowed({"USER", "ADMIN"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReadingResource {

    @Inject
    ReadingService readingService;

    @POST
    public Response create(@Valid CreateReadingRequest request, @Context SecurityContext sec) {
        UUID userId = UUID.fromString(sec.getUserPrincipal().getName());
        var appRequest = new com.library.reading.application.dto.CreateReadingRequest(request.bookId());
        var appResponse = readingService.create(userId, appRequest);
        return Response.status(201).entity(toApiResponse(appResponse)).build();
    }

    @GET
    public Response list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") String status,
            @Context SecurityContext sec) {
        UUID userId = UUID.fromString(sec.getUserPrincipal().getName());

        PageRequest pageRequest;
        try {
            pageRequest = PageRequest.of(page, size);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }

        ReadingStatus statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = ReadingStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status: " + status);
            }
        }

        PageResponse<com.library.reading.application.dto.ReadingResponse> appPage =
                readingService.listForUser(userId, statusFilter, pageRequest);

        PageResponse<ReadingResponse> apiPage = mapPage(appPage);
        return Response.ok(apiPage).build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") UUID id, @Context SecurityContext sec) {
        UUID userId = UUID.fromString(sec.getUserPrincipal().getName());
        var appResponse = readingService.getById(userId, id);
        return Response.ok(toApiResponse(appResponse)).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, UpdateReadingRequest request, @Context SecurityContext sec) {
        UUID userId = UUID.fromString(sec.getUserPrincipal().getName());
        boolean isAdmin = sec.isUserInRole("ADMIN");
        var appRequest = new com.library.reading.application.dto.UpdateReadingRequest(
                request.status(), request.rating(), request.review(), request.startedAt(), request.finishedAt());
        var appResponse = readingService.update(userId, id, appRequest, isAdmin);
        return Response.ok(toApiResponse(appResponse)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id, @Context SecurityContext sec) {
        UUID userId = UUID.fromString(sec.getUserPrincipal().getName());
        boolean isAdmin = sec.isUserInRole("ADMIN");
        readingService.delete(userId, id, isAdmin);
        return Response.noContent().build();
    }

    private ReadingResponse toApiResponse(com.library.reading.application.dto.ReadingResponse app) {
        BookSummary bookSummary = app.book() != null
                ? new BookSummary(app.book().id(), app.book().title())
                : null;
        return new ReadingResponse(
                app.id(),
                bookSummary,
                app.status(),
                app.rating(),
                app.review(),
                app.startedAt(),
                app.finishedAt(),
                app.createdAt(),
                app.updatedAt());
    }

    private PageResponse<ReadingResponse> mapPage(
            PageResponse<com.library.reading.application.dto.ReadingResponse> appPage) {
        var content = appPage.content().stream().map(this::toApiResponse).toList();
        return new PageResponse<>(content, appPage.totalElements(), appPage.totalPages(),
                appPage.page(), appPage.size());
    }
}
