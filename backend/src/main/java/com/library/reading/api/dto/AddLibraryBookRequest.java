package com.library.reading.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddLibraryBookRequest(
        @NotBlank @Size(max = 255) String googleBooksId,
        String status) {}
