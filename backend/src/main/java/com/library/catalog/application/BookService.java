package com.library.catalog.application;

import com.library.catalog.application.dto.BookRequest;
import com.library.catalog.application.dto.BookResponse;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;

import java.util.UUID;

public interface BookService {
    BookResponse create(BookRequest request);
    BookResponse update(UUID id, BookRequest request);
    void delete(UUID id);
    PageResponse<BookResponse> list(String search, PageRequest page);
    BookResponse getById(UUID id);
}
