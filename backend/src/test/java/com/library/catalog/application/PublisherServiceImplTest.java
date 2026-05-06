package com.library.catalog.application;

import com.library.catalog.application.dto.PublisherRequest;
import com.library.catalog.application.dto.PublisherResponse;
import com.library.catalog.domain.Publisher;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.NotFoundException;
import com.library.shared.exception.ValidationException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PublisherServiceImpl}.
 */
class PublisherServiceImplTest {

    private PublisherRepository publisherRepository;
    private PublisherServiceImpl service;

    @BeforeEach
    void setUp() {
        publisherRepository = mock(PublisherRepository.class);
        service = new PublisherServiceImpl();
        try {
            var field = PublisherServiceImpl.class.getDeclaredField("publisherRepository");
            field.setAccessible(true);
            field.set(service, publisherRepository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validName_persistsAndReturnsResponse() {
        PublisherRequest req = new PublisherRequest("Penguin Books");
        doAnswer(inv -> {
            Publisher p = inv.getArgument(0);
            p.id = UUID.randomUUID();
            p.createdAt = p.updatedAt = Instant.now();
            return null;
        }).when(publisherRepository).persist(any(Publisher.class));

        PublisherResponse resp = service.create(req);

        assertNotNull(resp.id());
        assertEquals("Penguin Books", resp.name());
        verify(publisherRepository).persist(any(Publisher.class));
    }

    @Test
    void create_blankName_throwsValidationException() {
        assertThrows(ValidationException.class, () -> service.create(new PublisherRequest("  ")));
        assertThrows(ValidationException.class, () -> service.create(new PublisherRequest("")));
        assertThrows(ValidationException.class, () -> service.create(new PublisherRequest(null)));
        verifyNoInteractions(publisherRepository);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_validName_updatesAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        Publisher existing = publisherWithId(id, "Old Name");
        when(publisherRepository.findByIdOrThrow(id)).thenReturn(existing);

        PublisherResponse resp = service.update(id, new PublisherRequest("New Name"));

        assertEquals("New Name", resp.name());
        verify(publisherRepository).persist(existing);
    }

    @Test
    void update_blankName_throwsValidationException() {
        UUID id = UUID.randomUUID();
        Publisher existing = publisherWithId(id, "Name");
        when(publisherRepository.findByIdOrThrow(id)).thenReturn(existing);

        assertThrows(ValidationException.class, () -> service.update(id, new PublisherRequest("")));
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(publisherRepository.findByIdOrThrow(id)).thenThrow(NotFoundException.of("Publisher", id));

        assertThrows(NotFoundException.class, () -> service.update(id, new PublisherRequest("Name")));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_noBooks_deletesPublisher() {
        UUID id = UUID.randomUUID();
        Publisher existing = publisherWithId(id, "Publisher");
        when(publisherRepository.findByIdOrThrow(id)).thenReturn(existing);
        when(publisherRepository.hasBooks(id)).thenReturn(false);

        service.delete(id);

        verify(publisherRepository).delete(existing);
    }

    @Test
    void delete_hasBooks_throwsConflictException() {
        UUID id = UUID.randomUUID();
        Publisher existing = publisherWithId(id, "Publisher");
        when(publisherRepository.findByIdOrThrow(id)).thenReturn(existing);
        when(publisherRepository.hasBooks(id)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(id));
        verify(publisherRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(publisherRepository.findByIdOrThrow(id)).thenThrow(NotFoundException.of("Publisher", id));

        assertThrows(NotFoundException.class, () -> service.delete(id));
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_delegatesToRepository() {
        PageRequest req = PageRequest.of(0, 10);
        PageResponse<PublisherResponse> expected = PageResponse.of(List.of(), 0L, req);
        when(publisherRepository.findAll(req)).thenReturn(expected);

        PageResponse<PublisherResponse> result = service.list(req);

        assertSame(expected, result);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_found_returnsResponse() {
        UUID id = UUID.randomUUID();
        Publisher publisher = publisherWithId(id, "Found Publisher");
        when(publisherRepository.findByIdOrThrow(id)).thenReturn(publisher);

        PublisherResponse resp = service.getById(id);

        assertEquals(id, resp.id());
        assertEquals("Found Publisher", resp.name());
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(publisherRepository.findByIdOrThrow(id)).thenThrow(NotFoundException.of("Publisher", id));

        assertThrows(NotFoundException.class, () -> service.getById(id));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Publisher publisherWithId(UUID id, String name) {
        Publisher p = new Publisher();
        p.id = id;
        p.name = name;
        p.createdAt = p.updatedAt = Instant.now();
        return p;
    }
}
