package com.library.reading.application.dto;

import java.util.UUID;

public record BookSummary(UUID id, String title, String publisher) {}
