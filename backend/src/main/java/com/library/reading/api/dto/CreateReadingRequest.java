package com.library.reading.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReadingRequest(@NotNull UUID bookId) {}
