package com.library.identity.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID userId,
        String displayName,
        String bio,
        List<String> favoriteGenres,
        Instant createdAt,
        Instant updatedAt) {
}
