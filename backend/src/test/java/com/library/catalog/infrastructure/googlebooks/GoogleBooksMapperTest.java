package com.library.catalog.infrastructure.googlebooks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoogleBooksMapperTest {

    private final GoogleBooksMapper mapper = new GoogleBooksMapper();

    @Test
    void mapsOptionalFieldsIdentifiersImagesAndSanitizedDescription() {
        var info = new GoogleBooksResponseDTO.VolumeInfo(
                " A  Paciente Silenciosa ", null, List.of("Alex Michaelides"), "Harper Collins",
                "2019", "<p>Um <strong>thriller</strong> psicológico.</p>",
                List.of(
                        new GoogleBooksResponseDTO.IndustryIdentifier("ISBN_10", "123-456789X"),
                        new GoogleBooksResponseDTO.IndustryIdentifier("ISBN_13", "978-0000000001")),
                350, List.of("Fiction", "Thrillers"),
                new GoogleBooksResponseDTO.ImageLinks(null, "http://image/thumbnail", null,
                        null, "http://image/large", null),
                "PT-BR", "http://books/info", "BOOK");

        var book = mapper.toCatalogBook(new GoogleBooksResponseDTO.Volume("abc123", info));

        assertEquals("A Paciente Silenciosa", book.title());
        assertEquals("123456789X", book.isbn10());
        assertEquals("9780000000001", book.isbn13());
        assertEquals("pt-br", book.language());
        assertEquals("Um thriller psicológico.", book.description());
        assertEquals("https://image/thumbnail", book.thumbnailUrl());
        assertEquals("https://image/large", book.largeCoverUrl());
        assertEquals("https://books/info", book.infoUrl());
        assertTrue(book.hasIsbn());
        assertTrue(book.metadataComplete());
    }

    @Test
    void acceptsVolumeWithoutAuthorIsbnImageOrPublisher() {
        var info = new GoogleBooksResponseDTO.VolumeInfo(
                "Obra antiga", null, null, null, null, null, null,
                null, null, null, "pt", null, "BOOK");

        var book = mapper.toCatalogBook(new GoogleBooksResponseDTO.Volume("old-volume", info));

        assertEquals(List.of(), book.authors());
        assertNull(book.publisher());
        assertNull(book.isbn10());
        assertNull(book.thumbnailUrl());
        assertFalse(book.hasIsbn());
        assertFalse(book.metadataComplete());
    }
}
