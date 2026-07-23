package com.library.reading.api;

import com.library.reading.api.dto.AddLibraryBookRequest;
import com.library.reading.application.LibraryService;
import com.library.reading.domain.ReadingStatus;
import com.library.shared.exception.ValidationException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.UUID;

@Path("/api/v1/library/books")
@RolesAllowed({"USER", "ADMIN"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LibraryResource {

    @Inject LibraryService libraryService;

    @POST
    public Response add(@Valid AddLibraryBookRequest request, @Context SecurityContext securityContext) {
        UUID userId = UUID.fromString(securityContext.getUserPrincipal().getName());
        ReadingStatus status = parseStatus(request.status());
        var response = libraryService.add(userId,
                new com.library.reading.application.dto.AddLibraryBookRequest(request.googleBooksId(), status));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    private ReadingStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return ReadingStatus.WANT_TO_READ;
        try {
            return ReadingStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Invalid status: " + value);
        }
    }
}
