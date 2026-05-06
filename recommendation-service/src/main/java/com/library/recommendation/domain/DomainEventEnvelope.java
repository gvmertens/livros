package com.library.recommendation.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirror of the monolith's DomainEventEnvelope — kept in sync manually.
 * The payload is deserialized from JSON as a generic Map for flexibility.
 */
public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Object payload
) {}
