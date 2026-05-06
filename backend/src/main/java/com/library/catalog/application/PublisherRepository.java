package com.library.catalog.application;

import com.library.catalog.application.dto.PublisherResponse;
import com.library.catalog.domain.Publisher;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface PublisherRepository {
    Optional<Publisher> findByIdOptional(UUID id);
    Publisher findByIdOrThrow(UUID id);
    boolean hasBooks(UUID publisherId);
    PageResponse<PublisherResponse> findAll(PageRequest page);
    void persist(Publisher publisher);
    void delete(Publisher publisher);
}
