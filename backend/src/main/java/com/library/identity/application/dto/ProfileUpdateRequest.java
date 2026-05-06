package com.library.identity.application.dto;

import java.util.List;

public record ProfileUpdateRequest(String displayName, String bio, List<String> favoriteGenres) {
}
