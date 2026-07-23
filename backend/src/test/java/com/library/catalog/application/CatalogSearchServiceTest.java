package com.library.catalog.application;

import com.library.catalog.infrastructure.googlebooks.GoogleBooksClient;
import com.library.catalog.infrastructure.googlebooks.GoogleBooksMapper;
import com.library.catalog.infrastructure.googlebooks.GoogleBooksProperties;
import com.library.catalog.infrastructure.googlebooks.GoogleBooksResponseDTO;
import com.library.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogSearchServiceTest {

    private GoogleBooksClient client;
    private CatalogSearchService service;

    @BeforeEach
    void setUp() {
        client = mock(GoogleBooksClient.class);
        GoogleBooksProperties properties = mock(GoogleBooksProperties.class);
        when(properties.maxResults()).thenReturn(20);
        service = new CatalogSearchService();
        service.client = client;
        service.mapper = new GoogleBooksMapper();
        service.properties = properties;
    }

    @Test
    void buildsQueriesForTextTitleAuthorAndIsbn() {
        when(client.search(anyString(), anyInt(), anyInt()))
                .thenReturn(new GoogleBooksResponseDTO(0, List.of()));

        service.search("crime", "Paciente", "Michaelides", "978-0-00000000-1", 0, 10);

        var query = ArgumentCaptor.forClass(String.class);
        verify(client).search(query.capture(), eq(0), eq(10));
        assertEquals("crime intitle:Paciente inauthor:Michaelides isbn:9780000000001", query.getValue());
    }

    @Test
    void prioritizesPortugueseAndRemovesDuplicateEditionsByIsbn13() {
        var english = volume("en-id", "English", "en", "9780000000001");
        var portuguese = volume("pt-id", "Português", "pt", "9780000000002");
        var duplicate = volume("duplicate-id", "Duplicado", "pt", "9780000000002");
        when(client.search(anyString(), anyInt(), anyInt()))
                .thenReturn(new GoogleBooksResponseDTO(3, List.of(english, portuguese, duplicate)));

        var result = service.search("thriller", null, null, null, 0, 20);

        assertEquals(2, result.total());
        assertEquals("pt-id", result.items().get(0).googleBooksId());
        assertEquals("en-id", result.items().get(1).googleBooksId());
    }

    @Test
    void rejectsEmptyCriteriaInvalidIsbnAndExcessiveLimit() {
        assertThrows(ValidationException.class,
                () -> service.search(null, null, null, null, 0, null));
        assertThrows(ValidationException.class,
                () -> service.search(null, null, null, "invalid", 0, null));
        assertThrows(ValidationException.class,
                () -> service.search("book", null, null, null, 0, 21));
    }

    private GoogleBooksResponseDTO.Volume volume(String id, String title, String language, String isbn13) {
        return new GoogleBooksResponseDTO.Volume(id, new GoogleBooksResponseDTO.VolumeInfo(
                title, null, List.of("Author"), "Publisher", "2020", "Description",
                List.of(new GoogleBooksResponseDTO.IndustryIdentifier("ISBN_13", isbn13)),
                300, List.of("Fiction"),
                new GoogleBooksResponseDTO.ImageLinks(null, "https://image", null, null, null, null),
                language, null, "BOOK"));
    }
}
