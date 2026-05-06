package com.library.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PublisherRequest(
        @NotBlank String name
) {}
