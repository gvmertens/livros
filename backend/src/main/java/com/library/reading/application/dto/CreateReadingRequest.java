package com.library.reading.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateReadingRequest(@NotNull UUID bookId) {}
