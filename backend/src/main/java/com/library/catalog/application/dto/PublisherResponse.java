package com.library.catalog.application.dto;

import java.time.Instant;
import java.util.UUID;

public record PublisherResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {}
