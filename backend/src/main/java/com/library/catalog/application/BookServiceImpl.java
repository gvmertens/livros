package com.library.catalog.application;

import com.library.catalog.application.dto.AuthorResponse;
import com.library.catalog.application.dto.BookRequest;
import com.library.catalog.application.dto.BookResponse;
import com.library.catalog.application.dto.PublisherResponse;
import com.library.catalog.application.event.BookCreatedPayload;
import com.library.catalog.application.event.BookDeletedPayload;
import com.library.catalog.application.event.BookUpdatedPayload;
import com.library.catalog.domain.Author;
import com.library.catalog.domain.Book;
import com.library.catalog.domain.Publisher;
import com.library.shared.event.DomainEventEnvelope;
import com.library.shared.event.EventBus;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.UnprocessableEntityException;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class BookServiceImpl implements BookService {

    @Inject
    BookRepository bookRepository;

    @Inject
    AuthorRepository authorRepository;

    @Inject
    PublisherRepository publisherRepository;

    @Inject
    EventBus eventBus;

    @Override
    @Transactional
    public BookResponse create(BookRequest request) {
        if (bookRepository.findByIsbn(request.isbn()).isPresent()) {
            throw new ConflictException("ISBN already exists: " + request.isbn());
        }

        Author author = authorRepository.findByIdOptional(request.authorId())
                .orElseThrow(() -> new UnprocessableEntityException("Author not found: " + request.authorId()));

        Publisher publisher = publisherRepository.findByIdOptional(request.publisherId())
                .orElseThrow(() -> new UnprocessableEntityException("Publisher not found: " + request.publisherId()));

        Book book = new Book();
        book.isbn = request.isbn();
        book.title = request.title();
        book.author = author;
        book.publisher = publisher;
        bookRepository.persist(book);

        eventBus.publish(new DomainEventEnvelope(
                UUID.randomUUID(),
                "book.created",
                Instant.now(),
                new BookCreatedPayload(book.id, book.isbn, book.title, book.author.id, book.publisher.id)));

        return toResponse(book);
    }

    @Override
    @Transactional
    public BookResponse update(UUID id, BookRequest request) {
        Book book = bookRepository.findByIdOrThrow(id);

        if (request.isbn() != null && !request.isbn().equals(book.isbn)) {
            bookRepository.findByIsbn(request.isbn()).ifPresent(existing -> {
                throw new ConflictException("ISBN already exists: " + request.isbn());
            });
            book.isbn = request.isbn();
        }

        if (request.title() != null) {
            book.title = request.title();
        }

        if (request.authorId() != null) {
            Author author = authorRepository.findByIdOptional(request.authorId())
                    .orElseThrow(() -> new UnprocessableEntityException("Author not found: " + request.authorId()));
            book.author = author;
        }

        if (request.publisherId() != null) {
            Publisher publisher = publisherRepository.findByIdOptional(request.publisherId())
                    .orElseThrow(() -> new UnprocessableEntityException("Publisher not found: " + request.publisherId()));
            book.publisher = publisher;
        }

        bookRepository.persist(book);

        eventBus.publish(new DomainEventEnvelope(
                UUID.randomUUID(),
                "book.updated",
                Instant.now(),
                new BookUpdatedPayload(book.id, book.isbn, book.title, book.author.id, book.publisher.id)));

        return toResponse(book);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Book book = bookRepository.findByIdOrThrow(id);
        bookRepository.delete(book);

        eventBus.publish(new DomainEventEnvelope(
                UUID.randomUUID(),
                "book.deleted",
                Instant.now(),
                new BookDeletedPayload(id)));
    }

    @Override
    public PageResponse<BookResponse> list(String search, PageRequest page) {
        return bookRepository.search(search, page);
    }

    @Override
    public BookResponse getById(UUID id) {
        Book book = bookRepository.findByIdOrThrow(id);
        return toResponse(book);
    }

    private BookResponse toResponse(Book book) {
        AuthorResponse authorResponse = new AuthorResponse(
                book.author.id, book.author.name, book.author.createdAt, book.author.updatedAt);
        PublisherResponse publisherResponse = new PublisherResponse(
                book.publisher.id, book.publisher.name, book.publisher.createdAt, book.publisher.updatedAt);
        return new BookResponse(
                book.id, book.isbn, book.title,
                authorResponse, publisherResponse,
                book.createdAt, book.updatedAt);
    }
}
