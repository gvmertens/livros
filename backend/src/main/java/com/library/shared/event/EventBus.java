package com.library.shared.event;

/**
 * Contract for publishing domain events within the application.
 *
 * <p>The default implementation ({@code InProcessEventBus}) dispatches events
 * synchronously via CDI observers. The interface is the only coupling point
 * between modules; swapping to an external broker (e.g. Kafka) requires only
 * a new {@code @Alternative} implementation — no module-level changes needed.
 */
public interface EventBus {

    /**
     * Publish a domain event envelope to all interested subscribers.
     *
     * @param event the envelope containing event metadata and payload
     */
    void publish(DomainEventEnvelope event);
}
