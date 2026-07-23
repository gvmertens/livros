package com.library.reading.application.event;

import java.util.UUID;

public record ReadingCreatedPayload(
        UUID readingId,
        UUID userId,
        UUID bookId,
        String bookTitle,
        String publisherName,
        String status) {}
