package com.library.catalog.api;

import com.library.catalog.api.dto.CatalogBookResponse;
import com.library.catalog.api.dto.CatalogSearchResponse;
import com.library.catalog.application.CatalogSearchService;
import com.library.catalog.application.dto.CatalogBook;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/catalog/books")
@RolesAllowed({"USER", "ADMIN"})
@Produces(MediaType.APPLICATION_JSON)
public class CatalogSearchResource {

    @Inject
    CatalogSearchService service;

    @GET
    public Response search(
            @QueryParam("query") String query,
            @QueryParam("title") String title,
            @QueryParam("author") String author,
            @QueryParam("isbn") String isbn,
            @QueryParam("startIndex") @DefaultValue("0") int startIndex,
            @QueryParam("maxResults") Integer maxResults) {
        var result = service.search(query, title, author, isbn, startIndex, maxResults);
        var items = result.items().stream().map(this::toResponse).toList();
        return Response.ok(new CatalogSearchResponse(items, result.total())).build();
    }

    @GET
    @Path("/{googleBooksId}")
    public Response findById(@PathParam("googleBooksId") String googleBooksId) {
        return Response.ok(toResponse(service.findByGoogleBooksId(googleBooksId))).build();
    }

    private CatalogBookResponse toResponse(CatalogBook book) {
        return new CatalogBookResponse(
                book.googleBooksId(), book.title(), book.subtitle(), book.authors(), book.publisher(),
                book.publishedDate(), book.isbn10(), book.isbn13(), book.language(),
                book.originalCategories(), book.description(), book.thumbnailUrl(), book.largeCoverUrl(),
                book.infoUrl(), book.pageCount(), book.printType(), book.hasIsbn(), book.metadataComplete());
    }
}
