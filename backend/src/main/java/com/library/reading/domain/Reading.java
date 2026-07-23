package com.library.reading.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "readings")
public class Reading extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "book_id", nullable = false)
    public UUID bookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ReadingStatus status = ReadingStatus.WANT_TO_READ;

    @Column(precision = 4, scale = 2)
    public BigDecimal rating;

    @Column(columnDefinition = "TEXT")
    public String review;

    @Column(nullable = false)
    public boolean favorite;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "started_at")
    public Instant startedAt;

    @Column(name = "finished_at")
    public Instant finishedAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
