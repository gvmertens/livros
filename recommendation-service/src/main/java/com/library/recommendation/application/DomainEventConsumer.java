package com.library.recommendation.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Kafka consumer for domain events published by the monolith.
 *
 * <p>Listens on the {@code domain-events} topic and processes:
 * <ul>
 *   <li>{@code rating.updated} — records the user's rating for a book</li>
 *   <li>{@code review.submitted} — records the user's review for a book</li>
 * </ul>
 *
 * <p>Other event types are silently ignored.
 *
 * Requirements: 9.1, 10.2
 */
@ApplicationScoped
public class DomainEventConsumer {

    private static final Logger log = Logger.getLogger(DomainEventConsumer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Inject
    UserReadingHistory history;

    @Incoming("domain-events-in")
    public void consume(String message) {
        try {
            JsonNode root = MAPPER.readTree(message);
            String eventType = root.path("eventType").asText();

            switch (eventType) {
                case "reading.created" -> handleReadingCreated(root.path("payload"));
                case "rating.updated"   -> handleRatingUpdated(root.path("payload"));
                case "review.submitted" -> handleReviewSubmitted(root.path("payload"));
                default -> log.debugf("Ignoring event type: %s", eventType);
            }
        } catch (Exception e) {
            log.errorf(e, "Failed to process domain event: %s", message);
            // Do not rethrow — a bad message should not stop the consumer
        }
    }

    private void handleReadingCreated(JsonNode payload) {
        UUID userId = UUID.fromString(payload.path("userId").asText());
        UUID bookId = UUID.fromString(payload.path("bookId").asText());
        history.record(userId, bookId, textOrNull(payload, "bookTitle"),
                textOrNull(payload, "publisherName"), null, null);
    }

    private void handleRatingUpdated(JsonNode payload) {
        UUID userId = UUID.fromString(payload.path("userId").asText());
        UUID bookId = UUID.fromString(payload.path("bookId").asText());
        double rating = payload.path("rating").asDouble();

        // Book title is not in the event payload — use bookId as fallback label
        history.record(userId, bookId, textOrNull(payload, "bookTitle"),
                textOrNull(payload, "publisherName"), rating, null);
        log.debugf("Recorded rating %.1f for user %s, book %s", rating, userId, bookId);
    }

    private void handleReviewSubmitted(JsonNode payload) {
        UUID userId = UUID.fromString(payload.path("userId").asText());
        UUID bookId = UUID.fromString(payload.path("bookId").asText());
        String review = payload.path("review").asText(null);

        history.record(userId, bookId, textOrNull(payload, "bookTitle"),
                textOrNull(payload, "publisherName"), null, review);
        log.debugf("Recorded review for user %s, book %s", userId, bookId);
    }

    private String textOrNull(JsonNode payload, String field) {
        String value = payload.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }
}
