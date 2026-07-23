package com.library.catalog.infrastructure.googlebooks;

import com.library.shared.exception.ExternalServiceException;
import com.library.shared.exception.NotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GoogleBooksClient {

    private static final Logger LOG = Logger.getLogger(GoogleBooksClient.class);

    @Inject
    @RestClient
    GoogleBooksApiClient api;

    @Inject
    GoogleBooksProperties properties;

    public GoogleBooksResponseDTO search(String query, int startIndex, int maxResults) {
        requireEnabled();
        long startedAt = System.nanoTime();
        try {
            LOG.debugf("Starting Google Books search [startIndex=%d, maxResults=%d]", startIndex, maxResults);
            GoogleBooksResponseDTO response = api.search(query, properties.defaultLanguage(), "books",
                    startIndex, maxResults, apiKey());
            int count = response == null || response.items() == null ? 0 : response.items().size();
            LOG.infof("Google Books search completed [durationMs=%d, results=%d]",
                    elapsedMillis(startedAt), count);
            return response == null ? new GoogleBooksResponseDTO(0, java.util.List.of()) : response;
        } catch (ProcessingException e) {
            throw temporaryFailure(e, startedAt);
        } catch (WebApplicationException e) {
            throw temporaryFailure(e, startedAt);
        }
    }

    public GoogleBooksResponseDTO.Volume findById(String volumeId) {
        requireEnabled();
        long startedAt = System.nanoTime();
        try {
            LOG.debugf("Starting Google Books volume lookup [volumeId=%s]", volumeId);
            GoogleBooksResponseDTO.Volume volume = api.findById(volumeId, apiKey());
            if (volume == null || volume.id() == null) {
                throw NotFoundException.of("Google Books volume", volumeId);
            }
            LOG.infof("Google Books volume lookup completed [durationMs=%d]", elapsedMillis(startedAt));
            return volume;
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 404) {
                throw NotFoundException.of("Google Books volume", volumeId);
            }
            throw temporaryFailure(e, startedAt);
        } catch (ProcessingException e) {
            throw temporaryFailure(e, startedAt);
        }
    }

    private void requireEnabled() {
        if (!properties.enabled()) {
            throw new ExternalServiceException("Google Books integration is disabled");
        }
    }

    private String apiKey() {
        return properties.apiKey().filter(value -> !value.isBlank()).orElse(null);
    }

    private ExternalServiceException temporaryFailure(Exception cause, long startedAt) {
        LOG.warnf(cause, "Google Books request failed [durationMs=%d]", elapsedMillis(startedAt));
        return new ExternalServiceException("Google Books is temporarily unavailable");
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
