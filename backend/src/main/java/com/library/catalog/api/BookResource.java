package com.library.catalog.api;

import com.library.catalog.api.dto.AuthorResponse;
import com.library.catalog.api.dto.BookRequest;
import com.library.catalog.api.dto.BookResponse;
import com.library.catalog.api.dto.PublisherResponse;
import com.library.catalog.application.BookService;
import com.library.shared.exception.ValidationException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {

    @Inject
    BookService bookService;

    @POST
    @RolesAllowed("ADMIN")
    public Response create(@Valid BookRequest request) {
        var appRequest = new com.library.catalog.application.dto.BookRequest(
                request.isbn(), request.title(), request.authorId(), request.publisherId());
        var appResponse = bookService.create(appRequest);
        return Response.status(201).entity(toApiResponse(appResponse)).build();
    }

    @GET
    @RolesAllowed({"USER", "ADMIN"})
    public Response list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") @DefaultValue("") String search) {
        PageRequest pageRequest;
        try {
            pageRequest = PageRequest.of(page, size);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        PageResponse<com.library.catalog.application.dto.BookResponse> appPage = bookService.list(search, pageRequest);
        PageResponse<BookResponse> apiPage = mapPage(appPage);
        return Response.ok(apiPage).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"USER", "ADMIN"})
    public Response getById(@PathParam("id") UUID id) {
        var appResponse = bookService.getById(id);
        return Response.ok(toApiResponse(appResponse)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response update(@PathParam("id") UUID id, @Valid BookRequest request) {
        var appRequest = new com.library.catalog.application.dto.BookRequest(
                request.isbn(), request.title(), request.authorId(), request.publisherId());
        var appResponse = bookService.update(id, appRequest);
        return Response.ok(toApiResponse(appResponse)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") UUID id) {
        bookService.delete(id);
        return Response.noContent().build();
    }

    private BookResponse toApiResponse(com.library.catalog.application.dto.BookResponse app) {
        AuthorResponse author = new AuthorResponse(
                app.author().id(), app.author().name(), app.author().createdAt(), app.author().updatedAt());
        PublisherResponse publisher = new PublisherResponse(
                app.publisher().id(), app.publisher().name(), app.publisher().createdAt(), app.publisher().updatedAt());
        return new BookResponse(app.id(), app.isbn(), app.title(), author, publisher, app.createdAt(), app.updatedAt());
    }

    private PageResponse<BookResponse> mapPage(PageResponse<com.library.catalog.application.dto.BookResponse> appPage) {
        var content = appPage.content().stream().map(this::toApiResponse).toList();
        return new PageResponse<>(content, appPage.totalElements(), appPage.totalPages(), appPage.page(), appPage.size());
    }
}
