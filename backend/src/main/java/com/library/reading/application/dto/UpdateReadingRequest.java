package com.library.reading.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateReadingRequest(
        String status,
        BigDecimal rating,
        String review,
        Instant startedAt,
        Instant finishedAt
) {}
