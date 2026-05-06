package com.library.catalog.application;

import com.library.catalog.application.dto.AuthorRequest;
import com.library.catalog.application.dto.AuthorResponse;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;

import java.util.UUID;

public interface AuthorService {
    AuthorResponse create(AuthorRequest request);
    AuthorResponse update(UUID id, AuthorRequest request);
    void delete(UUID id);
    PageResponse<AuthorResponse> list(PageRequest page);
    AuthorResponse getById(UUID id);
}
