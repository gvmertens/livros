package com.library.catalog.application;

import com.library.catalog.application.dto.PublisherRequest;
import com.library.catalog.application.dto.PublisherResponse;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;

import java.util.UUID;

public interface PublisherService {
    PublisherResponse create(PublisherRequest request);
    PublisherResponse update(UUID id, PublisherRequest request);
    void delete(UUID id);
    PageResponse<PublisherResponse> list(PageRequest page);
    PublisherResponse getById(UUID id);
}
