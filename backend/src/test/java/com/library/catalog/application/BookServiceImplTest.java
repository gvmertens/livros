package com.library.catalog.application;

import com.library.catalog.application.dto.AuthorResponse;
import com.library.catalog.application.dto.BookRequest;
import com.library.catalog.application.dto.BookResponse;
import com.library.catalog.application.dto.PublisherResponse;
import com.library.catalog.domain.Author;
import com.library.catalog.domain.Book;
import com.library.catalog.domain.Publisher;
import com.library.shared.event.DomainEventEnvelope;
import com.library.shared.event.EventBus;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.NotFoundException;
import com.library.shared.exception.UnprocessableEntityException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookServiceImpl}.
 */
class BookServiceImplTest {

    private BookRepository bookRepository;
    private AuthorRepository authorRepository;
    private PublisherRepository publisherRepository;
    private EventBus eventBus;
    private BookServiceImpl service;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        authorRepository = mock(AuthorRepository.class);
        publisherRepository = mock(PublisherRepository.class);
        eventBus = mock(EventBus.class);
        service = new BookServiceImpl();
        try {
            setField("bookRepository", bookRepository);
            setField("authorRepository", authorRepository);
            setField("publisherRepository", publisherRepository);
            setField("eventBus", eventBus);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(String name, Object value) throws Exception {
        var field = BookServiceImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_persistsAndPublishesEvent() {
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        BookRequest req = new BookRequest("9780261102354", "The Lord of the Rings", authorId, publisherId);

        when(bookRepository.findByIsbn("9780261102354")).thenReturn(Optional.empty());
        Author author = authorWithId(authorId, "Tolkien");
        Publisher publisher = publisherWithId(publisherId, "Allen & Unwin");
        when(authorRepository.findByIdOptional(authorId)).thenReturn(Optional.of(author));
        when(publisherRepository.findByIdOptional(publisherId)).thenReturn(Optional.of(publisher));
        doAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.id = UUID.randomUUID();
            b.createdAt = b.updatedAt = Instant.now();
            return null;
        }).when(bookRepository).persist(any(Book.class));

        BookResponse resp = service.create(req);

        assertNotNull(resp.id());
        assertEquals("9780261102354", resp.isbn());
        assertEquals("The Lord of the Rings", resp.title());
        assertEquals(authorId, resp.author().id());
        assertEquals(publisherId, resp.publisher().id());

        ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(eventBus).publish(captor.capture());
        assertEquals("book.created", captor.getValue().eventType());
    }

    @Test
    void create_duplicateIsbn_throwsConflictException() {
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        BookRequest req = new BookRequest("9780261102354", "Title", authorId, publisherId);
        when(bookRepository.findByIsbn("9780261102354")).thenReturn(Optional.of(new Book()));

        assertThrows(ConflictException.class, () -> service.create(req));
        verify(bookRepository, never()).persist(any());
        verifyNoInteractions(eventBus);
    }

    @Test
    void create_authorNotFound_throwsUnprocessableEntityException() {
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        BookRequest req = new BookRequest("1234567890123", "Title", authorId, publisherId);
        when(bookRepository.findByIsbn(any())).thenReturn(Optional.empty());
        when(authorRepository.findByIdOptional(authorId)).thenReturn(Optional.empty());

        assertThrows(UnprocessableEntityException.class, () -> service.create(req));
        verifyNoInteractions(eventBus);
    }

    @Test
    void create_publisherNotFound_throwsUnprocessableEntityException() {
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        BookRequest req = new BookRequest("1234567890123", "Title", authorId, publisherId);
        when(bookRepository.findByIsbn(any())).thenReturn(Optional.empty());
        when(authorRepository.findByIdOptional(authorId)).thenReturn(Optional.of(authorWithId(authorId, "Author")));
        when(publisherRepository.findByIdOptional(publisherId)).thenReturn(Optional.empty());

        assertThrows(UnprocessableEntityException.class, () -> service.create(req));
        verifyNoInteractions(eventBus);
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_validRequest_updatesAndPublishesEvent() {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        Book existing = bookWithId(bookId, "9780261102354", "Old Title",
                authorWithId(authorId, "Author"), publisherWithId(publisherId, "Publisher"));
        when(bookRepository.findByIdOrThrow(bookId)).thenReturn(existing);

        UUID newAuthorId = UUID.randomUUID();
        Author newAuthor = authorWithId(newAuthorId, "New Author");
        when(authorRepository.findByIdOptional(newAuthorId)).thenReturn(Optional.of(newAuthor));
        when(publisherRepository.findByIdOptional(publisherId)).thenReturn(Optional.of(publisherWithId(publisherId, "Publisher")));

        BookRequest req = new BookRequest("9780261102354", "New Title", newAuthorId, publisherId);
        BookResponse resp = service.update(bookId, req);

        assertEquals("New Title", resp.title());
        assertEquals(newAuthorId, resp.author().id());

        ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(eventBus).publish(captor.capture());
        assertEquals("book.updated", captor.getValue().eventType());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findByIdOrThrow(bookId)).thenThrow(NotFoundException.of("Book", bookId));

        assertThrows(NotFoundException.class, () ->
                service.update(bookId, new BookRequest("isbn", "title", UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void update_duplicateIsbn_throwsConflictException() {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        Book existing = bookWithId(bookId, "OLD_ISBN", "Title",
                authorWithId(authorId, "Author"), publisherWithId(publisherId, "Publisher"));
        when(bookRepository.findByIdOrThrow(bookId)).thenReturn(existing);
        when(bookRepository.findByIsbn("NEW_ISBN")).thenReturn(Optional.of(new Book()));

        assertThrows(ConflictException.class, () ->
                service.update(bookId, new BookRequest("NEW_ISBN", "Title", authorId, publisherId)));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_existingBook_deletesAndPublishesEvent() {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        Book existing = bookWithId(bookId, "isbn", "Title",
                authorWithId(authorId, "Author"), publisherWithId(publisherId, "Publisher"));
        when(bookRepository.findByIdOrThrow(bookId)).thenReturn(existing);

        service.delete(bookId);

        verify(bookRepository).delete(existing);
        ArgumentCaptor<DomainEventEnvelope> captor = ArgumentCaptor.forClass(DomainEventEnvelope.class);
        verify(eventBus).publish(captor.capture());
        assertEquals("book.deleted", captor.getValue().eventType());
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findByIdOrThrow(bookId)).thenThrow(NotFoundException.of("Book", bookId));

        assertThrows(NotFoundException.class, () -> service.delete(bookId));
        verifyNoInteractions(eventBus);
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_delegatesToRepository() {
        PageRequest req = PageRequest.of(0, 20);
        PageResponse<BookResponse> expected = PageResponse.of(List.of(), 0L, req);
        when(bookRepository.search("", req)).thenReturn(expected);

        PageResponse<BookResponse> result = service.list("", req);

        assertSame(expected, result);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_found_returnsResponse() {
        UUID bookId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        Book book = bookWithId(bookId, "isbn", "Title",
                authorWithId(authorId, "Author"), publisherWithId(publisherId, "Publisher"));
        when(bookRepository.findByIdOrThrow(bookId)).thenReturn(book);

        BookResponse resp = service.getById(bookId);

        assertEquals(bookId, resp.id());
        assertEquals("isbn", resp.isbn());
    }

    @Test
    void getById_notFound_throwsNotFoundException() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findByIdOrThrow(bookId)).thenThrow(NotFoundException.of("Book", bookId));

        assertThrows(NotFoundException.class, () -> service.getById(bookId));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Author authorWithId(UUID id, String name) {
        Author a = new Author();
        a.id = id;
        a.name = name;
        a.createdAt = a.updatedAt = Instant.now();
        return a;
    }

    private Publisher publisherWithId(UUID id, String name) {
        Publisher p = new Publisher();
        p.id = id;
        p.name = name;
        p.createdAt = p.updatedAt = Instant.now();
        return p;
    }

    private Book bookWithId(UUID id, String isbn, String title, Author author, Publisher publisher) {
        Book b = new Book();
        b.id = id;
        b.isbn = isbn;
        b.title = title;
        b.author = author;
        b.publisher = publisher;
        b.createdAt = b.updatedAt = Instant.now();
        return b;
    }
}
