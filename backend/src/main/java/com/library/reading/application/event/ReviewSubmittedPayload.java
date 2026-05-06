package com.library.reading.application.event;

import java.util.UUID;

public record ReviewSubmittedPayload(UUID readingId, UUID userId, UUID bookId, String review) {}
