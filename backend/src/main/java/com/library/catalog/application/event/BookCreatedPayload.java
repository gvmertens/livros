package com.library.catalog.application.event;

import java.util.UUID;

public record BookCreatedPayload(
        UUID bookId,
        String isbn,
        String title,
        UUID authorId,
        UUID publisherId
) {}
