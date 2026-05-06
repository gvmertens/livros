package com.library.identity.application.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String role, Instant createdAt) {
}
