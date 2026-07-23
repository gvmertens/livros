package com.library.recommendation.application;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of reading events per user.
 *
 * <p>Accumulates rating and review events consumed from Kafka so the
 * recommendation engine has context when building prompts.
 *
 * <p>In a production deployment this would be backed by a persistent store
 * (e.g. Redis or PostgreSQL) to survive restarts.
 */
@ApplicationScoped
public class UserReadingHistory {

    public record ReadingEntry(UUID bookId, String bookTitle, String publisherName,
                               Double rating, String review) {}

    private final Map<UUID, Map<UUID, ReadingEntry>> history = new ConcurrentHashMap<>();

    /** Record a rating or review event for a user. */
    public void record(UUID userId, UUID bookId, String bookTitle, String publisherName,
                       Double rating, String review) {
        history.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>())
                .compute(bookId, (ignored, current) -> new ReadingEntry(
                        bookId,
                        bookTitle != null ? bookTitle : current != null ? current.bookTitle() : null,
                        publisherName != null ? publisherName : current != null ? current.publisherName() : null,
                        rating != null ? rating : current != null ? current.rating() : null,
                        review != null ? review : current != null ? current.review() : null));
    }

    /** Returns all recorded entries for a user (unmodifiable). */
    public List<ReadingEntry> getHistory(UUID userId) {
        Map<UUID, ReadingEntry> entries = history.get(userId);
        return entries == null ? List.of() : List.copyOf(entries.values());
    }
}
