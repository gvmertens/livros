package com.library.infrastructure;

import com.library.shared.event.DomainEventEnvelope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CDI observer that captures all {@link DomainEventEnvelope} events published
 * via the in-process {@code EventBus} during integration tests.
 *
 * <p>Tests can call {@link #getEvents()} to assert that specific events were
 * published, and {@link #clear()} to reset state between test methods.
 *
 * <p>This bean is {@code @ApplicationScoped} so it is shared across all tests
 * within a single Quarkus test application instance.
 */
@ApplicationScoped
public class TestEventObserver {

    private final List<DomainEventEnvelope> events = new CopyOnWriteArrayList<>();

    /** Called by CDI whenever any domain event is published. */
    public void onEvent(@Observes DomainEventEnvelope envelope) {
        events.add(envelope);
    }

    /** Returns an unmodifiable snapshot of all captured events. */
    public List<DomainEventEnvelope> getEvents() {
        return List.copyOf(events);
    }

    /**
     * Returns all events whose {@code eventType} matches the given string.
     *
     * @param eventType the event type to filter on (e.g. {@code "user.created"})
     */
    public List<DomainEventEnvelope> getEventsOfType(String eventType) {
        return events.stream()
                .filter(e -> eventType.equals(e.eventType()))
                .toList();
    }

    /** Clears all captured events. Call this in {@code @BeforeEach} or {@code @AfterEach}. */
    public void clear() {
        events.clear();
    }
}
