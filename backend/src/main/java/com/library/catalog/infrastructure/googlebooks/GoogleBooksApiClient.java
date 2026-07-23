package com.library.catalog.infrastructure.googlebooks;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "google-books")
@Path("/volumes")
@Produces(MediaType.APPLICATION_JSON)
public interface GoogleBooksApiClient {

    @GET
    GoogleBooksResponseDTO search(
            @QueryParam("q") String query,
            @QueryParam("langRestrict") String language,
            @QueryParam("printType") String printType,
            @QueryParam("startIndex") int startIndex,
            @QueryParam("maxResults") int maxResults,
            @QueryParam("key") String apiKey);

    @GET
    @Path("/{volumeId}")
    GoogleBooksResponseDTO.Volume findById(
            @PathParam("volumeId") String volumeId,
            @QueryParam("key") String apiKey);
}
