package com.library.reading.application;

import com.library.catalog.application.AuthorRepository;
import com.library.catalog.application.BookRepository;
import com.library.catalog.application.CatalogSearchService;
import com.library.catalog.application.PublisherRepository;
import com.library.catalog.application.dto.CatalogBook;
import com.library.catalog.domain.Author;
import com.library.catalog.domain.Book;
import com.library.catalog.domain.BookSource;
import com.library.catalog.domain.Publisher;
import com.library.reading.application.dto.AddLibraryBookRequest;
import com.library.reading.application.dto.CreateReadingRequest;
import com.library.reading.application.dto.ReadingResponse;
import com.library.reading.application.dto.UpdateReadingRequest;
import com.library.reading.domain.ReadingStatus;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class LibraryService {

    @Inject CatalogSearchService catalogSearchService;
    @Inject BookRepository bookRepository;
    @Inject AuthorRepository authorRepository;
    @Inject PublisherRepository publisherRepository;
    @Inject ReadingRepository readingRepository;
    @Inject ReadingService readingService;

    @Transactional
    public ReadingResponse add(UUID userId, AddLibraryBookRequest request) {
        if (request == null || request.googleBooksId() == null || request.googleBooksId().isBlank()) {
            throw new ValidationException("googleBooksId must not be blank");
        }
        ReadingStatus status = request.status() == null ? ReadingStatus.WANT_TO_READ : request.status();
        if (status == ReadingStatus.FINISHED) {
            throw new ValidationException("FINISHED status requires reading dates and must be set after inclusion");
        }

        // Always reload the selected volume. Metadata sent by clients is never trusted.
        CatalogBook catalogBook = catalogSearchService.findByGoogleBooksId(request.googleBooksId().trim());
        Book book = findExisting(catalogBook);
        if (book == null) {
            book = createBook(catalogBook);
        }

        if (readingRepository.findByUserIdAndBookId(userId, book.id).isPresent()) {
            throw new ConflictException("Book edition already exists in this user's library");
        }

        ReadingResponse created = readingService.create(userId, new CreateReadingRequest(book.id));
        if (status == ReadingStatus.WANT_TO_READ) {
            return created;
        }
        return readingService.update(userId, created.id(),
                new UpdateReadingRequest(status.name(), null, null, null, null), false);
    }

    private Book findExisting(CatalogBook source) {
        if (source.isbn13() != null) {
            var match = bookRepository.findByIsbn13(source.isbn13());
            if (match.isPresent()) return match.get();
        }
        if (source.isbn10() != null) {
            var match = bookRepository.findByIsbn10(source.isbn10());
            if (match.isPresent()) return match.get();
        }
        var externalMatch = bookRepository.findByGoogleBooksId(source.googleBooksId());
        if (externalMatch.isPresent()) return externalMatch.get();

        String author = source.authors().isEmpty() ? "" : normalize(source.authors().get(0));
        return bookRepository.findByNormalizedMetadata(
                normalize(source.title()), author, normalize(source.publisher())).orElse(null);
    }

    private Book createBook(CatalogBook source) {
        Book book = new Book();
        book.googleBooksId = source.googleBooksId();
        book.title = source.title();
        book.subtitle = source.subtitle();
        book.description = source.description();
        book.isbn10 = source.isbn10();
        book.isbn13 = source.isbn13();
        book.isbn = source.isbn13() != null ? source.isbn13() : source.isbn10();
        book.language = source.language();
        book.publishedDate = source.publishedDate();
        book.pageCount = source.pageCount();
        book.externalCategories = new ArrayList<>(source.originalCategories());
        book.thumbnailUrl = source.thumbnailUrl();
        book.largeCoverUrl = source.largeCoverUrl();
        book.infoUrl = source.infoUrl();
        book.printType = source.printType();
        book.dataSource = BookSource.GOOGLE_BOOKS;
        book.metadataUpdatedAt = Instant.now();
        book.normalizedTitle = normalize(source.title());

        for (String authorName : source.authors()) {
            Author author = authorRepository.findByNormalizedName(authorName)
                    .orElseGet(() -> createAuthor(authorName));
            book.authors.add(author);
        }
        if (!book.authors.isEmpty()) {
            book.author = book.authors.get(0); // legacy compatibility during transition
            book.normalizedPrimaryAuthor = normalize(book.author.name);
        } else {
            book.normalizedPrimaryAuthor = "";
        }

        if (source.publisher() != null) {
            book.publisher = publisherRepository.findByNormalizedName(source.publisher())
                    .orElseGet(() -> createPublisher(source.publisher()));
        }
        book.normalizedPublisher = normalize(source.publisher());

        try {
            bookRepository.persist(book);
            bookRepository.flush();
            return book;
        } catch (PersistenceException exception) {
            throw new ConflictException("Book edition was created concurrently");
        }
    }

    private Author createAuthor(String name) {
        Author author = new Author();
        author.name = name;
        authorRepository.persist(author);
        return author;
    }

    private Publisher createPublisher(String name) {
        Publisher publisher = new Publisher();
        publisher.name = name;
        publisherRepository.persist(publisher);
        return publisher;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
