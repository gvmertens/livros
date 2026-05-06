package com.library.shared.event;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * In-process CDI-based implementation of {@link EventBus}.
 *
 * <p>Dispatches events synchronously within the same JVM using CDI observers.
 * To swap to an external broker (e.g. Kafka), provide a new
 * {@code @Alternative @Priority(1)} implementation — no service changes needed.
 */
@ApplicationScoped
public class InProcessEventBus implements EventBus {

    @Inject
    Event<DomainEventEnvelope> cdiEvent;

    @Override
    public void publish(DomainEventEnvelope envelope) {
        cdiEvent.fire(envelope);
    }
}
