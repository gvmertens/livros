package com.library.catalog.application.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthorResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {}
