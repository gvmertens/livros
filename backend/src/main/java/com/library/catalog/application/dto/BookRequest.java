package com.library.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BookRequest(
        @NotBlank @Size(max = 13) String isbn,
        @NotBlank @Size(max = 500) String title,
        @NotNull UUID authorId,
        @NotNull UUID publisherId
) {}
