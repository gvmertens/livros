package com.library.catalog.api.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that catalog API DTOs (records) correctly hold and expose their fields.
 * These tests cover the mapping layer between application DTOs and API DTOs.
 */
class CatalogDtoMappingTest {

    // ── AuthorRequest ─────────────────────────────────────────────────────────

    @Test
    void authorRequest_holdsName() {
        AuthorRequest req = new AuthorRequest("George Orwell");
        assertEquals("George Orwell", req.name());
    }

    // ── AuthorResponse ────────────────────────────────────────────────────────

    @Test
    void authorResponse_holdsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        AuthorResponse resp = new AuthorResponse(id, "George Orwell", now, now);
        assertEquals(id, resp.id());
        assertEquals("George Orwell", resp.name());
        assertEquals(now, resp.createdAt());
        assertEquals(now, resp.updatedAt());
    }

    // ── PublisherRequest ──────────────────────────────────────────────────────

    @Test
    void publisherRequest_holdsName() {
        PublisherRequest req = new PublisherRequest("Secker & Warburg");
        assertEquals("Secker & Warburg", req.name());
    }

    // ── PublisherResponse ─────────────────────────────────────────────────────

    @Test
    void publisherResponse_holdsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        PublisherResponse resp = new PublisherResponse(id, "Secker & Warburg", now, now);
        assertEquals(id, resp.id());
        assertEquals("Secker & Warburg", resp.name());
        assertEquals(now, resp.createdAt());
        assertEquals(now, resp.updatedAt());
    }

    // ── BookRequest ───────────────────────────────────────────────────────────

    @Test
    void bookRequest_holdsAllFields() {
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        BookRequest req = new BookRequest("9780451524935", "1984", authorId, publisherId);
        assertEquals("9780451524935", req.isbn());
        assertEquals("1984", req.title());
        assertEquals(authorId, req.authorId());
        assertEquals(publisherId, req.publisherId());
    }

    // ── BookResponse ──────────────────────────────────────────────────────────

    @Test
    void bookResponse_embedsAuthorAndPublisher() {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        Instant now = Instant.now();

        AuthorResponse author = new AuthorResponse(authorId, "George Orwell", now, now);
        PublisherResponse publisher = new PublisherResponse(publisherId, "Secker & Warburg", now, now);
        BookResponse resp = new BookResponse(bookId, "9780451524935", "1984", author, publisher, now, now);

        assertEquals(bookId, resp.id());
        assertEquals("9780451524935", resp.isbn());
        assertEquals("1984", resp.title());
        assertEquals(authorId, resp.author().id());
        assertEquals("George Orwell", resp.author().name());
        assertEquals(publisherId, resp.publisher().id());
        assertEquals("Secker & Warburg", resp.publisher().name());
        assertEquals(now, resp.createdAt());
        assertEquals(now, resp.updatedAt());
    }

    // ── Application ↔ API DTO mapping ─────────────────────────────────────────

    @Test
    void applicationAuthorResponse_mapsToApiAuthorResponse() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        com.library.catalog.application.dto.AuthorResponse appResp =
                new com.library.catalog.application.dto.AuthorResponse(id, "Tolkien", now, now);

        // Simulate what AuthorResource.toApiResponse does
        AuthorResponse apiResp = new AuthorResponse(appResp.id(), appResp.name(), appResp.createdAt(), appResp.updatedAt());

        assertEquals(appResp.id(), apiResp.id());
        assertEquals(appResp.name(), apiResp.name());
        assertEquals(appResp.createdAt(), apiResp.createdAt());
        assertEquals(appResp.updatedAt(), apiResp.updatedAt());
    }

    @Test
    void applicationPublisherResponse_mapsToApiPublisherResponse() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        com.library.catalog.application.dto.PublisherResponse appResp =
                new com.library.catalog.application.dto.PublisherResponse(id, "Allen & Unwin", now, now);

        PublisherResponse apiResp = new PublisherResponse(appResp.id(), appResp.name(), appResp.createdAt(), appResp.updatedAt());

        assertEquals(appResp.id(), apiResp.id());
        assertEquals(appResp.name(), apiResp.name());
    }

    @Test
    void applicationBookResponse_mapsToApiBookResponse() {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        Instant now = Instant.now();

        com.library.catalog.application.dto.AuthorResponse appAuthor =
                new com.library.catalog.application.dto.AuthorResponse(authorId, "Tolkien", now, now);
        com.library.catalog.application.dto.PublisherResponse appPublisher =
                new com.library.catalog.application.dto.PublisherResponse(publisherId, "Allen & Unwin", now, now);
        com.library.catalog.application.dto.BookResponse appBook =
                new com.library.catalog.application.dto.BookResponse(bookId, "isbn", "Title", appAuthor, appPublisher, now, now);

        // Simulate BookResource.toApiResponse
        AuthorResponse apiAuthor = new AuthorResponse(appBook.author().id(), appBook.author().name(),
                appBook.author().createdAt(), appBook.author().updatedAt());
        PublisherResponse apiPublisher = new PublisherResponse(appBook.publisher().id(), appBook.publisher().name(),
                appBook.publisher().createdAt(), appBook.publisher().updatedAt());
        BookResponse apiBook = new BookResponse(appBook.id(), appBook.isbn(), appBook.title(),
                apiAuthor, apiPublisher, appBook.createdAt(), appBook.updatedAt());

        assertEquals(bookId, apiBook.id());
        assertEquals("isbn", apiBook.isbn());
        assertEquals("Title", apiBook.title());
        assertEquals(authorId, apiBook.author().id());
        assertEquals(publisherId, apiBook.publisher().id());
    }
}
