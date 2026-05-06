package com.library.reading.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReadingResponse(
        UUID id,
        BookSummary book,
        String status,
        BigDecimal rating,
        String review,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Instant updatedAt
) {}
