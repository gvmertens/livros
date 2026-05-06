package com.library.recommendation.application;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Collections;
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

    public record ReadingEntry(String bookTitle, Double rating, String review) {}

    private final Map<UUID, List<ReadingEntry>> history = new ConcurrentHashMap<>();

    /** Record a rating or review event for a user. */
    public void record(UUID userId, String bookTitle, Double rating, String review) {
        history.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
               .add(new ReadingEntry(bookTitle, rating, review));
    }

    /** Returns all recorded entries for a user (unmodifiable). */
    public List<ReadingEntry> getHistory(UUID userId) {
        return List.copyOf(history.getOrDefault(userId, List.of()));
    }
}
