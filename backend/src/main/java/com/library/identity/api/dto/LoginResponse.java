package com.library.identity.api.dto;

public record LoginResponse(String token, long expiresIn) {
}
