package com.library.identity.api.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record ProfileUpdateRequest(
        @Size(max = 100) String displayName,
        @Size(max = 1000) String bio,
        List<String> favoriteGenres) {
}
