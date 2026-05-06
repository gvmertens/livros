package com.library.identity.application.dto;

public record LoginResponse(String token, long expiresIn) {
}
