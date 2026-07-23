package com.library.reading.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record RatingUpdatedPayload(UUID readingId, UUID userId, UUID bookId,
        String bookTitle, String publisherName, BigDecimal rating) {}
