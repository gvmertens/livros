package com.library.catalog.application;

import com.library.catalog.application.dto.AuthorResponse;
import com.library.catalog.domain.Author;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository {
    Optional<Author> findByNormalizedName(String name);
    Optional<Author> findByIdOptional(UUID id);
    Author findByIdOrThrow(UUID id);
    boolean hasBooks(UUID authorId);
    PageResponse<AuthorResponse> findAll(PageRequest page);
    void persist(Author author);
    void delete(Author author);
}
