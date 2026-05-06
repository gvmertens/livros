package com.library.reading.application;

import com.library.reading.application.dto.CreateReadingRequest;
import com.library.reading.application.dto.ReadingResponse;
import com.library.reading.application.dto.UpdateReadingRequest;
import com.library.reading.domain.ReadingStatus;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;

import java.util.UUID;

public interface ReadingService {

    ReadingResponse create(UUID userId, CreateReadingRequest request);

    ReadingResponse update(UUID requestingUserId, UUID readingId, UpdateReadingRequest request, boolean isAdmin);

    void delete(UUID requestingUserId, UUID readingId, boolean isAdmin);

    PageResponse<ReadingResponse> listForUser(UUID userId, ReadingStatus statusFilter, PageRequest page);

    ReadingResponse getById(UUID requestingUserId, UUID readingId);
}
