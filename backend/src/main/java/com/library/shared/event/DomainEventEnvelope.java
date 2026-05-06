package com.library.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope wrapping a domain event with metadata for routing and auditing.
 * The payload is serialized to JSON on publish by the EventBus implementation.
 *
 * @param eventId     unique identifier for this event instance
 * @param eventType   fully-qualified or logical name of the event (e.g. "catalog.BookAdded")
 * @param occurredAt  timestamp when the event occurred
 * @param payload     the domain event payload; serialized to JSON on publish
 */
public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Object payload
) {
}
