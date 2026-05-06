package com.library.catalog.application.event;

import java.util.UUID;

public record BookDeletedPayload(
        UUID bookId
) {}
