package com.library.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorRequest(
        @NotBlank String name
) {}
