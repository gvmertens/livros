package com.library.catalog.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String isbn,
        String title,
        AuthorResponse author,
        PublisherResponse publisher,
        Instant createdAt,
        Instant updatedAt
) {}
