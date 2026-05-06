package com.library.identity.application.event;

import java.time.Instant;
import java.util.UUID;

public record UserCreatedPayload(UUID userId, String email, Instant createdAt) {
}
