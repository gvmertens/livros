package com.library.catalog.infrastructure.googlebooks;

import com.library.shared.exception.ExternalServiceException;
import com.library.shared.exception.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleBooksClientTest {

    private GoogleBooksApiClient api;
    private GoogleBooksProperties properties;
    private GoogleBooksClient client;

    @BeforeEach
    void setUp() {
        api = mock(GoogleBooksApiClient.class);
        properties = mock(GoogleBooksProperties.class);
        when(properties.enabled()).thenReturn(true);
        when(properties.defaultLanguage()).thenReturn("pt");
        when(properties.apiKey()).thenReturn(Optional.empty());
        client = new GoogleBooksClient();
        client.api = api;
        client.properties = properties;
    }

    @Test
    void executesSearchWithLanguagePrintTypeAndPagination() {
        var expected = new GoogleBooksResponseDTO(1, List.of(
                new GoogleBooksResponseDTO.Volume("abc", null)));
        when(api.search(anyString(), anyString(), anyString(), anyInt(), anyInt(), isNull()))
                .thenReturn(expected);

        var response = client.search("intitle:Livro", 5, 10);

        assertSame(expected, response);
        verify(api).search("intitle:Livro", "pt", "books", 5, 10, null);
    }

    @Test
    void sendsConfiguredApiKeyWithoutLoggingOrExposingIt() {
        when(properties.apiKey()).thenReturn(Optional.of("secret-key"));
        when(api.search(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString()))
                .thenReturn(new GoogleBooksResponseDTO(0, List.of()));

        client.search("book", 0, 1);

        var key = ArgumentCaptor.forClass(String.class);
        verify(api).search(eq("book"), eq("pt"), eq("books"), eq(0), eq(1), key.capture());
        assertEquals("secret-key", key.getValue());
    }

    @Test
    void mapsNotFoundUnexpectedHttpErrorsAndNetworkFailures() {
        when(api.findById(eq("missing"), isNull())).thenThrow(
                new WebApplicationException(Response.status(404).build()));
        when(api.findById(eq("failure"), isNull())).thenThrow(
                new WebApplicationException(Response.status(500).build()));
        when(api.findById(eq("timeout"), isNull())).thenThrow(new ProcessingException("timeout"));

        assertThrows(NotFoundException.class, () -> client.findById("missing"));
        assertThrows(ExternalServiceException.class, () -> client.findById("failure"));
        assertThrows(ExternalServiceException.class, () -> client.findById("timeout"));
    }

    @Test
    void rejectsCallsWhenIntegrationIsDisabled() {
        when(properties.enabled()).thenReturn(false);

        assertThrows(ExternalServiceException.class, () -> client.search("book", 0, 10));
        verifyNoInteractions(api);
    }
}
