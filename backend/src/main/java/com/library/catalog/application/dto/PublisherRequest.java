package com.library.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PublisherRequest(
        @NotBlank String name
) {}
