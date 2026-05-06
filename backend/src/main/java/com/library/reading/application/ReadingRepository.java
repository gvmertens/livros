package com.library.reading.application;

import com.library.reading.domain.Reading;
import com.library.reading.domain.ReadingStatus;
import com.library.shared.pagination.PageRequest;
import com.library.shared.pagination.PageResponse;

import java.util.Optional;
import java.util.UUID;

public interface ReadingRepository {

    Optional<Reading> findByUserIdAndBookId(UUID userId, UUID bookId);

    Reading findByIdOrThrow(UUID id);

    PageResponse<Reading> findByUserId(UUID userId, ReadingStatus status, PageRequest page);

    void persist(Reading reading);

    void delete(Reading reading);
}
