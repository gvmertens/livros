package com.library.reading.application;

import com.library.catalog.application.AuthorRepository;
import com.library.catalog.application.BookRepository;
import com.library.catalog.application.CatalogSearchService;
import com.library.catalog.application.PublisherRepository;
import com.library.catalog.application.dto.CatalogBook;
import com.library.catalog.domain.Book;
import com.library.catalog.domain.BookSource;
import com.library.reading.application.dto.AddLibraryBookRequest;
import com.library.reading.application.dto.BookSummary;
import com.library.reading.application.dto.ReadingResponse;
import com.library.reading.domain.Reading;
import com.library.reading.domain.ReadingStatus;
import com.library.shared.exception.ConflictException;
import com.library.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LibraryServiceTest {

    private CatalogSearchService catalog;
    private BookRepository books;
    private AuthorRepository authors;
    private PublisherRepository publishers;
    private ReadingRepository readings;
    private ReadingService readingService;
    private LibraryService service;

    @BeforeEach
    void setUp() {
        catalog = mock(CatalogSearchService.class);
        books = mock(BookRepository.class);
        authors = mock(AuthorRepository.class);
        publishers = mock(PublisherRepository.class);
        readings = mock(ReadingRepository.class);
        readingService = mock(ReadingService.class);
        service = new LibraryService();
        service.catalogSearchService = catalog;
        service.bookRepository = books;
        service.authorRepository = authors;
        service.publisherRepository = publishers;
        service.readingRepository = readings;
        service.readingService = readingService;
    }

    @Test
    void reloadsVolumeCreatesBibliographicRecordAndAssociatesUser() {
        UUID userId = UUID.randomUUID();
        CatalogBook source = catalogBook();
        when(catalog.findByGoogleBooksId("google-1")).thenReturn(source);
        when(books.findByIsbn13(source.isbn13())).thenReturn(Optional.empty());
        when(books.findByIsbn10(source.isbn10())).thenReturn(Optional.empty());
        when(books.findByGoogleBooksId(source.googleBooksId())).thenReturn(Optional.empty());
        when(books.findByNormalizedMetadata(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(authors.findByNormalizedName("Alex Michaelides")).thenReturn(Optional.empty());
        when(publishers.findByNormalizedName("Harper Collins")).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.id = UUID.randomUUID();
            return null;
        }).when(books).persist(any(Book.class));
        when(readings.findByUserIdAndBookId(eq(userId), any(UUID.class))).thenReturn(Optional.empty());
        when(readingService.create(eq(userId), any())).thenAnswer(invocation -> {
            var request = (com.library.reading.application.dto.CreateReadingRequest) invocation.getArgument(1);
            return response(request.bookId(), ReadingStatus.WANT_TO_READ);
        });

        ReadingResponse result = service.add(userId,
                new AddLibraryBookRequest("google-1", ReadingStatus.WANT_TO_READ));

        assertEquals(ReadingStatus.WANT_TO_READ.name(), result.status());
        verify(catalog).findByGoogleBooksId("google-1");
        verify(books).persist(argThat(book ->
                book.dataSource == BookSource.GOOGLE_BOOKS
                        && "A Paciente Silenciosa".equals(book.title)
                        && "9780000000001".equals(book.isbn13)
                        && book.authors.size() == 1
                        && book.publisher != null));
        verify(books).flush();
        verify(readingService).create(eq(userId), any());
    }

    @Test
    void reusesExistingEditionByIsbnAndDoesNotPersistClientMetadata() {
        UUID userId = UUID.randomUUID();
        Book existing = new Book();
        existing.id = UUID.randomUUID();
        CatalogBook source = catalogBook();
        when(catalog.findByGoogleBooksId("google-1")).thenReturn(source);
        when(books.findByIsbn13(source.isbn13())).thenReturn(Optional.of(existing));
        when(readings.findByUserIdAndBookId(userId, existing.id)).thenReturn(Optional.empty());
        when(readingService.create(eq(userId), any())).thenReturn(response(existing.id, ReadingStatus.WANT_TO_READ));

        service.add(userId, new AddLibraryBookRequest("google-1", null));

        verify(books, never()).persist(any());
        verify(readingService).create(eq(userId),
                argThat(request -> existing.id.equals(request.bookId())));
    }

    @Test
    void rejectsDuplicateEditionForSameUser() {
        UUID userId = UUID.randomUUID();
        Book existing = new Book();
        existing.id = UUID.randomUUID();
        CatalogBook source = catalogBook();
        when(catalog.findByGoogleBooksId("google-1")).thenReturn(source);
        when(books.findByIsbn13(source.isbn13())).thenReturn(Optional.of(existing));
        when(readings.findByUserIdAndBookId(userId, existing.id)).thenReturn(Optional.of(new Reading()));

        assertThrows(ConflictException.class,
                () -> service.add(userId, new AddLibraryBookRequest("google-1", null)));
        verifyNoInteractions(readingService);
    }

    @Test
    void rejectsFinishedAsInitialStatusAndBlankExternalId() {
        assertThrows(ValidationException.class,
                () -> service.add(UUID.randomUUID(), new AddLibraryBookRequest(" ", null)));
        assertThrows(ValidationException.class,
                () -> service.add(UUID.randomUUID(),
                        new AddLibraryBookRequest("google-1", ReadingStatus.FINISHED)));
        verifyNoInteractions(catalog);
    }

    private CatalogBook catalogBook() {
        return new CatalogBook(
                "google-1", "A Paciente Silenciosa", null, List.of("Alex Michaelides"),
                "Harper Collins", "2019", "0000000001", "9780000000001", "pt",
                List.of("Fiction", "Thrillers"), "Descrição", "https://cover/small",
                "https://cover/large", "https://books/info", 350, "BOOK", true, true);
    }

    private ReadingResponse response(UUID bookId, ReadingStatus status) {
        Instant now = Instant.now();
        return new ReadingResponse(UUID.randomUUID(), new BookSummary(bookId, "Title", "Publisher"),
                status.name(), null, null, null, null, now, now);
    }
}
