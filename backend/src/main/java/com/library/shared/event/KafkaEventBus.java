package com.library.shared.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Kafka-backed implementation of {@link EventBus}.
 *
 * <p>Activated as a CDI {@code @Alternative} with priority 1, which overrides
 * the default {@link InProcessEventBus} when this class is on the classpath
 * and Kafka is configured.
 *
 * <p>Publishes every {@link DomainEventEnvelope} as a JSON string to the
 * {@code domain-events} Kafka topic. The event type is used as the Kafka
 * message key so consumers can filter by event type efficiently.
 *
 * <p>To activate: set {@code quarkus.kafka.enabled=true} (or configure the
 * bootstrap servers) in {@code application.properties}. The in-process bus
 * remains active in dev/test when Kafka is not configured.
 *
 * Requirements: 9.1, 10.2
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class KafkaEventBus implements EventBus {

    private static final Logger log = Logger.getLogger(KafkaEventBus.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Inject
    @Channel("domain-events-out")
    Emitter<String> emitter;

    @Override
    public void publish(DomainEventEnvelope envelope) {
        try {
            String json = MAPPER.writeValueAsString(envelope);

            // Use eventType as Kafka message key for consumer-side filtering
            var metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(envelope.eventType())
                    .build();

            emitter.send(Message.of(json).addMetadata(metadata));

            log.debugf("Published event [type=%s, id=%s] to Kafka",
                    envelope.eventType(), envelope.eventId());

        } catch (JsonProcessingException e) {
            log.errorf(e, "Failed to serialize event [type=%s, id=%s]",
                    envelope.eventType(), envelope.eventId());
            throw new RuntimeException("Failed to publish event to Kafka", e);
        }
    }
}
