package com.library.catalog.application;

import com.library.catalog.application.dto.AuthorRequest;
import com.library.catalog.application.dto.AuthorResponse;
import com.library.catalog.domain.Author;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.NotFoundException;
import com.library.shared.exception.ValidationException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthorServiceImpl}.
 * Uses Mockito to isolate the service from the repository.
 */
class AuthorServiceImplTest {

    private AuthorRepository authorRepository;
    private AuthorServiceImpl service;

    @BeforeEach
    void setUp() {
        authorRepository = mock(AuthorRepository.class);
        service = new AuthorServiceImpl();
        // Inject mock via reflection (field injection)
        try {
            var field = AuthorServiceImpl.class.getDeclaredField("authorRepository");
            field.setAccessible(true);
            field.set(service, authorRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validName_persistsAndReturnsResponse() {
        AuthorRequest req = new AuthorRequest("J.R.R. Tolkien");
        doAnswer(inv -> {
            Author a = inv.getArgument(0);
            a.id = UUID.randomUUID();
            a.createdAt = a.updatedAt = Instant.now();
            return null;
        }).when(authorRepository).persist(any(Author.class));

        AuthorResponse resp = service.create(req);

        assertNotNull(resp.id());
        assertEquals("J.R.R. Tolkien", resp.name());
        verify(authorRepository).persist(any(Author.class));
    }

    @Test
    void create_blankName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.create(new AuthorRequest("  ")));
        assertThrows(ValidationException.class, () -> service.create(new AuthorRequest("")));
        assertThrows(ValidationException.class, () -> service.create(new AuthorRequest(null)));
        verifyNoInteractions(authorRepository);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_validName_updatesAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        Author existing = authorWithId(id, "Old Name");
        when(authorRepository.findByIdOrThrow(id)).thenReturn(existing);

        AuthorResponse resp = service.update(id, new AuthorRequest("New Name"));

        assertEquals("New Name", resp.name());
        verify(authorRepository).persist(existing);
    }

    @Test
    void update_blankName_throwsValidationException() {
        UUID id = UUID.randomUUID();
        Author existing = authorWithId(id, "Name");
        when(authorRepository.findByIdOrThrow(id)).thenReturn(existing);

        assertThrows(ValidationException.class, () -> service.update(id, new AuthorRequest("")));
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authorRepository.findByIdOrThrow(id)).thenThrow(NotFoundException.of("Author", id));

        assertThrows(NotFoundException.class, () -> service.update(id, new AuthorRequest("Name")));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_noBooks_deletesAuthor() {
        UUID id = UUID.randomUUID();
        Author existing = authorWithId(id, "Author");
        when(authorRepository.findByIdOrThrow(id)).thenReturn(existing);
        when(authorRepository.hasBooks(id)).thenReturn(false);

        service.delete(id);

        verify(authorRepository).delete(existing);
    }

    @Test
    void delete_hasBooks_throwsConflictException() {
        UUID id = UUID.randomUUID();
        Author existing = authorWithId(id, "Author");
        when(authorRepository.findByIdOrThrow(id)).thenReturn(existing);
        when(authorRepository.hasBooks(id)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(id));
        verify(authorRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authorRepository.findByIdOrThrow(id)).thenThrow(NotFoundException.of("Author", id));

        assertThrows(NotFoundException.class, () -> service.delete(id));
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_delegatesToRepository() {
        PageRequest req = PageRequest.of(0, 10);
        PageResponse<AuthorResponse> expected = PageResponse.of(List.of(), 0L, req);
        when(authorRepository.findAll(req)).thenReturn(expected);

        PageResponse<AuthorResponse> result = service.list(req);

        assertSame(expected, result);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_found_returnsResponse() {
        UUID id = UUID.randomUUID();
        Author author = authorWithId(id, "Found Author");
        when(authorRepository.findByIdOrThrow(id)).thenReturn(author);

        AuthorResponse resp = service.getById(id);

        assertEquals(id, resp.id());
        assertEquals("Found Author", resp.name());
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(authorRepository.findByIdOrThrow(id)).thenThrow(NotFoundException.of("Author", id));

        assertThrows(NotFoundException.class, () -> service.getById(id));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Author authorWithId(UUID id, String name) {
        Author a = new Author();
        a.id = id;
        a.name = name;
        a.createdAt = a.updatedAt = Instant.now();
        return a;
    }
}
