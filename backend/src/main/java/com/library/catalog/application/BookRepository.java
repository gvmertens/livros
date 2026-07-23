package com.library.catalog.application;

import com.library.catalog.application.dto.BookResponse;
import com.library.catalog.domain.Book;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository {
    Optional<Book> findByIsbn(String isbn);
    Optional<Book> findByGoogleBooksId(String googleBooksId);
    Optional<Book> findByIsbn10(String isbn10);
    Optional<Book> findByIsbn13(String isbn13);
    Optional<Book> findByIdOptional(UUID id);
    Book findByIdOrThrow(UUID id);
    PageResponse<BookResponse> search(String query, PageRequest page);
    void persist(Book book);
    void delete(Book book);
}
